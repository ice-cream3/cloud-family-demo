#!/usr/bin/env python3
"""Development regression for application-enforced relationships, including two races.
Uses only new uniquely named fixtures; deletes those fixtures in finally.
Set MANAGER_TEST_URL and GATEWAY_INTERNAL_TOKEN for the target development service.
"""
import concurrent.futures
import json
import os
import threading
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get('MANAGER_TEST_URL', 'http://127.0.0.1:38083')
PREFIX = 'FK_TEST_' + uuid.uuid4().hex[:10].upper()
PERMISSIONS = ','.join('system:' + resource + ':' + action
                       for resource in ('user', 'role', 'menu', 'permission', 'dict')
                       for action in ('view', 'add', 'edit', 'delete', 'menu', 'button-permission'))
fixtures = []
checks = 0


def call(path, body=None, expected=(200, 201)):
    headers = {'Content-Type': 'application/json',
               'X-Gateway-Internal-Token': os.environ.get('GATEWAY_INTERNAL_TOKEN', 'cloud-family-demo-gateway-internal-token'),
               'X-User-Name': PREFIX, 'X-User-Type': 'MANAGER', 'X-User-Permissions': PERMISSIONS}
    request = urllib.request.Request(BASE + '/api/manager/system/' + path,
                                    data=json.dumps(body or {}).encode(), headers=headers, method='POST')
    try:
        response = urllib.request.urlopen(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    payload = json.loads(response.read())
    assert response.code in expected, (path, response.code, payload)
    if response.code < 300:
        assert payload['code'] == 0, (path, payload)
    return response.code, payload.get('data')


def create(kind, body):
    _, record = call(kind, body)
    fixtures.append((kind, record['id']))
    return record['id']


def remove(kind, record_id):
    call(kind + '/delete/' + str(record_id))
    fixtures.remove((kind, record_id))


def check(name):
    global checks
    checks += 1
    print('PASS:', name)


def menu(parent=None):
    return {'parentId': parent, 'menuCode': PREFIX + '_' + uuid.uuid4().hex[:8],
            'menuName': '关系约束验证', 'buttonFlag': False, 'sortOrder': 0}


try:
    user = create('users', {'username': PREFIX.lower(), 'password': uuid.uuid4().hex,
                            'displayName': '关系约束验证'})
    role = create('roles', {'roleCode': PREFIX, 'roleName': '关系约束验证'})
    permission = create('permissions', {'permissionCode': PREFIX, 'permissionName': '关系约束验证'})
    parent = create('menus', menu())
    child = create('menus', menu(parent))
    call(f'menus/delete/{parent}', expected=(400,))
    call(f'menus/update/{parent}', menu(child), expected=(400,))
    call('menus', menu(-999999), expected=(404,))
    check('missing parent, parent deletion and ancestor cycle rejected')
    call(f'users/roles/replace/{user}', {'ids': [role]})
    call(f'users/roles/replace/{user}', {'ids': [-999999]}, expected=(404,))
    assert [r['id'] for r in call(f'users/roles/{user}')[1]] == [role]
    call(f'roles/permissions/replace/{role}', {'ids': [permission]})
    call(f'roles/permissions/replace/{role}', {'ids': [-999999]}, expected=(404,))
    assert [p['id'] for p in call(f'roles/permissions/{role}')[1]] == [permission]
    call(f'roles/menus/replace/{role}', {'ids': [child]})
    call(f'menus/permissions/replace/{child}', {'ids': [permission]})
    check('nonexistent assignment target rejected without losing valid associations')
    remove('permissions', permission)
    assert call(f'roles/permissions/{role}')[1] == []
    assert call(f'menus/permissions/{child}')[1] == []
    check('permission deletion cleans role and menu links')
    remove('menus', child)
    assert call(f'roles/menus/{role}')[1] == []
    remove('menus', parent)
    check('leaf menu deletion cleans role links; empty parent can be deleted')
    remove('roles', role)
    assert call(f'users/roles/{user}')[1] == []
    check('role deletion cleans user links')
    for attempt in range(2):
        race_role = create('roles', {'roleCode': PREFIX + '_R' + str(attempt), 'roleName': '并发关系验证'})
        barrier = threading.Barrier(2)
        def assign():
            barrier.wait()
            return call(f'users/roles/replace/{user}', {'ids': [race_role]}, expected=(200, 404))[0]
        def delete_role():
            barrier.wait()
            return call(f'roles/delete/{race_role}')[0]
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            a, b = executor.submit(assign), executor.submit(delete_role)
            a.result(); b.result()
        fixtures.remove(('roles', race_role))
        assert call(f'users/roles/{user}')[1] == []
    check('concurrent role assignment/deletion does not create dangling links')
    for _ in range(2):
        race_parent = create('menus', menu())
        barrier = threading.Barrier(2)
        def add_child():
            barrier.wait()
            return call('menus', menu(race_parent), expected=(201, 404))
        def delete_parent():
            barrier.wait()
            return call(f'menus/delete/{race_parent}', expected=(200, 400))[0]
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            a, b = executor.submit(add_child), executor.submit(delete_parent)
            child_status, child_record = a.result()
            delete_status = b.result()
        assert not (child_status == 201 and delete_status == 200)
        if child_status == 201:
            fixtures.append(('menus', child_record['id']))
            remove('menus', child_record['id'])
        if delete_status == 200:
            fixtures.remove(('menus', race_parent))
        else:
            remove('menus', race_parent)
    check('concurrent child creation/parent deletion is serialized correctly')
    cleanup_role = create('roles', {'roleCode': PREFIX + '_CLEANUP', 'roleName': '用户删除清理验证'})
    call(f'users/roles/replace/{user}', {'ids': [cleanup_role]})
    remove('users', user)
    remove('roles', cleanup_role)
    check('user deletion succeeds with role links; final orphan audit checks cleanup')
    print('All', checks, 'API relationship checks passed')
finally:
    errors = []
    for kind, record_id in reversed(fixtures[:]):
        try:
            remove(kind, record_id)
        except Exception as error:
            errors.append((kind, record_id, str(error)))
    assert not errors, ('Fixture cleanup failed', errors)
