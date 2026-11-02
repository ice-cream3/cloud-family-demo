#!/usr/bin/env python3
"""Smoke tests against temporary local services and two NEW test-only users.
Supply COMMERCE_TEST_USER_A/B (must start __commerce_test_), COMMERCE_INTERNAL_TOKEN,
PARTNER_TEST_URL and MANAGER_TEST_URL. Run only in a development database.
The script creates test shops/products and removes those records in finally.
It does not create or delete accounts. Never uses existing shops.
"""
import json
import os
import urllib.request
import urllib.error

A = os.environ['COMMERCE_TEST_USER_A']
B = os.environ['COMMERCE_TEST_USER_B']
assert A.startswith('__commerce_test_') and B.startswith('__commerce_test_') and A != B
PARTNER = os.environ.get('PARTNER_TEST_URL', 'http://localhost:48082')
MANAGER = os.environ.get('MANAGER_TEST_URL', 'http://localhost:48083')
TOKEN = os.environ.get('COMMERCE_INTERNAL_TOKEN', 'cloud-family-demo-gateway-internal-token')
PERMS = ','.join('partner:showcase:' + action for action in ('view', 'add', 'edit', 'delete'))
ROOT = '/api/users/commerce'
ADMIN = '/api/manager/partner/features/'
checks = 0

def call(path, body=None, user=A, expected=200, permissions=None):
    manager = path.startswith('/api/manager')
    headers = {'X-Gateway-Internal-Token': TOKEN, 'X-User-Name': user, 'X-User-Type': 'MANAGER' if manager else 'API', 'Content-Type': 'application/json'}
    if manager:
        headers['X-User-Permissions'] = permissions if permissions is not None else PERMS
    request = urllib.request.Request((MANAGER if manager else PARTNER) + path, data=json.dumps(body or {}).encode(), headers=headers, method='POST')
    try:
        response = urllib.request.urlopen(request, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    payload = json.loads(response.read())
    if not manager and expected in (400, 403, 404):
        assert response.code == 200 and payload.get('code') == expected * 100, (path, response.code, payload)
    else:
        assert response.code == expected and payload.get('code') == (0 if expected == 200 else expected * 100), (path, response.code, payload)
    return payload.get('data')

def ok(name):
    global checks
    checks += 1
    print('PASS:', name)

shop_a = shop_b = product = None
try:
    assert call(ROOT + '/mine/shop') is None and call(ROOT + '/mine/shop', user=B) is None
    shop_a = call(ROOT + '/mine/shop/save', {'name': A, 'description': '测试商家A', 'contact': '测试公开联系方式', 'ownerUsername': B, 'status': 'BLOCKED'})
    shop_b = call(ROOT + '/mine/shop/save', {'name': B, 'description': '测试商家B'}, user=B)
    assert shop_a['ownerUsername'] == A and shop_a['status'] == 'ACTIVE' and shop_a['id'] != shop_b['id']
    ok('shop creation binds authenticated owner and ignores supplied admin status')
    value = {'title': A + '-商品', 'category': '测试分类', 'summary': '商品简介', 'content': '商品详情\n第二段', 'price': '19.90', 'stock': '5', 'sortOrder': '0', 'status': 'DRAFT', 'shopId': shop_b['id'], 'ownerUsername': B}
    call(ROOT + '/mine/products', value)
    result = call(ROOT + '/mine/products/page')
    assert result['total'] == 1
    product = result['records'][0]
    pid = product['id']
    assert product['shopId'] == shop_a['id'] and float(product['price']) == 19.9
    call(ROOT + '/products/detail/' + str(pid), expected=404)
    ok('draft product is private and cannot select another merchant')
    value['status'] = 'PUBLISHED'
    call(ROOT + '/mine/products/update/' + str(pid), value)
    detail = call(ROOT + '/products/detail/' + str(pid))
    assert detail['shopName'] == A and detail['content'] == '商品详情\n第二段'
    page = call(ROOT + '/shops/' + str(shop_a['id']) + '/products/page', {'keyword': A, 'category': '测试分类', 'pageSize': 1})
    assert page['total'] == 1 and len(page['records']) == 1
    assert call(ROOT + '/shops/' + str(shop_a['id']) + '/categories') == ['测试分类']
    assert call(ROOT + '/shops/' + str(shop_b['id']) + '/products/page')['total'] == 0
    ok('merchant -> product list -> detail, category/search and merchant isolation')
    call(ROOT + '/mine/products/update/' + str(pid), value, user=B, expected=400)
    call(ROOT + '/mine/products/delete/' + str(pid), user=B, expected=400)
    assert call(ROOT + '/mine/products/page', user=B)['total'] == 0
    ok('another user cannot modify, delete or list the seller products')
    for invalid in ({'price': '-1'}, {'price': '1.001'}, {'stock': '-1'}, {'stock': '1.5'}, {'coverUrl': 'javascript:alert(1)'}):
        call(ROOT + '/mine/products', {**value, **invalid}, expected=400)
    ok('negative/fractional prices and stock plus unsafe cover rejected')
    admin_products = call(ADMIN + 'commerce-products/page?keyword=' + A)
    assert admin_products['total'] == 1
    admin_value = {**value, 'shopId': shop_a['id'], 'moderationStatus': 'BLOCKED', 'moderationReason': '商品描述不完整，请完善后联系管理员'}
    call(ADMIN + 'commerce-products/update/' + str(pid), admin_value, expected=403, permissions='partner:showcase:view')
    call(ADMIN + 'commerce-products/update/' + str(pid), {**admin_value, 'moderationReason': ''}, expected=400)
    call(ADMIN + 'commerce-products/update/' + str(pid), admin_value)
    blocked = call(ROOT + '/mine/products/page')['records'][0]
    assert blocked['moderationStatus'] == 'BLOCKED' and blocked['status'] == 'OFFLINE'
    assert blocked['moderationReason'] == admin_value['moderationReason']
    assert call(ROOT + '/mine/products/page', {'status': 'SYSTEM_BLOCKED'})['total'] == 1
    call(ROOT + '/products/detail/' + str(pid), expected=404)
    call(ROOT + '/mine/products/update/' + str(pid), {**value, 'moderationStatus': 'ACTIVE'}, expected=400)
    ok('admin all-user list, write permissions and blocked product cannot self-unblock')
    value['status'] = 'OFFLINE'
    call(ROOT + '/mine/products/update/' + str(pid), {**value, 'moderationStatus': 'ACTIVE', 'moderationReason': '试图篡改原因'})
    still_blocked = call(ROOT + '/mine/products/page')['records'][0]
    assert still_blocked['moderationStatus'] == 'BLOCKED' and still_blocked['moderationReason'] == admin_value['moderationReason']
    admin_value.update(status='OFFLINE', moderationStatus='ACTIVE')
    call(ADMIN + 'commerce-products/update/' + str(pid), admin_value)
    value['status'] = 'PUBLISHED'
    call(ROOT + '/mine/products/update/' + str(pid), value)
    call(ROOT + '/products/detail/' + str(pid))
    ok('offline transition and admin restore preserve separate seller/admin states')
    shop_admin = {**shop_a, 'status': 'BLOCKED', 'sortOrder': 0}
    call(ADMIN + 'commerce-shops/update/' + str(shop_a['id']), shop_admin)
    assert call(ROOT + '/shops/page', {'keyword': A})['total'] == 0
    call(ROOT + '/shops/' + str(shop_a['id']), expected=404)
    call(ROOT + '/products/detail/' + str(pid), expected=404)
    call(ROOT + '/mine/products/update/' + str(pid), value, expected=400)
    own = call(ROOT + '/mine/shop/save', {'name': A, 'description': '更新资料', 'status': 'ACTIVE'})
    assert own['status'] == 'BLOCKED'
    ok('blocked shop hides all products and seller profile changes cannot unblock')
    shop_admin['status'] = 'ACTIVE'
    call(ADMIN + 'commerce-shops/update/' + str(shop_a['id']), shop_admin)
    assert call(ROOT + '/shops/page', {'keyword': A})['records'][0]['productCount'] == 1
    call(ADMIN + 'commerce-shops/delete/' + str(shop_a['id']), expected=400)
    ok('admin restore and non-empty shop deletion guard')
    assert call(ROOT + '/shops/' + str(shop_a['id']) + '/products/page', {'keyword': A, 'pageNum': 2, 'pageSize': 1})['records'] == []
    call(ROOT + '/mine/products/delete/' + str(pid))
    product = None
    assert call(ROOT + '/mine/products/page')['total'] == 0
    call(ROOT + '/products/detail/' + str(pid), expected=404)
    ok('pagination and owner deletion')
    print('PASS:', checks, 'commerce HTTP scenarios')
finally:
    if product:
        call(ROOT + '/mine/products/delete/' + str(product['id']))
    for shop in (shop_a, shop_b):
        if shop:
            call(ADMIN + 'commerce-shops/delete/' + str(shop['id']))
    print('Test-created shops/products cleaned up')
