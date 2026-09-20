import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { ChevronRight, Eye, EyeOff, FolderTree, KeyRound, Layers3, ListTree, LogOut, Pencil, Plus, RefreshCw, Search, Trash2, Users, X } from 'lucide-react';
import type { ManagerDashboard, MenuTreeNode, OperationLog, PageQuery, PageResult, SysPermission, SysRole, SystemPageRecord, SystemRecordPayload, UserProfile } from '../types/api';
import type { Session } from '../services/tokenStore';
import { loadSystemMenuTree } from '../services/dashboardService';

type DashboardProps = {
  session: Session;
  managerDashboard: ManagerDashboard | null;
  userProfile: UserProfile | null;
  menuTree: MenuTreeNode[];
  menuError: string | null;
  activeMenu: MenuTreeNode | null;
  systemPage: PageResult<SystemPageRecord> | null;
  systemPageQuery: PageQuery;
  systemPageLoading: boolean;
  systemPageError: string | null;
  onMenuSelect: (menu: MenuTreeNode) => void;
  onSystemPageChange: (pageNum: number) => void;
  onSystemPageSearch: (query: PageQuery) => void;
  onCreateSystemRecord: (payload: SystemRecordPayload) => Promise<void>;
  onUpdateSystemRecord: (id: number, payload: SystemRecordPayload) => Promise<void>;
  onDeleteSystemRecord: (id: number) => Promise<void>;
  onResetUserPassword: (id: number, password: string) => Promise<void>;
  onLoadMenuPermissionOptions: () => Promise<SysPermission[]>;
  onLoadMenuPermissions: (id: number) => Promise<SysPermission[]>;
  onReplaceMenuPermissions: (id: number, ids: number[]) => Promise<void>;
  onLoadUserRoleOptions: () => Promise<SysRole[]>;
  onLoadUserRoles: (id: number) => Promise<SysRole[]>;
  onReplaceUserRoles: (id: number, ids: number[]) => Promise<void>;
  loading: boolean;
  error: string | null;
  onReload: () => void;
  onLogout: () => void;
};

export function Dashboard({
  session,
  managerDashboard,
  userProfile,
  menuTree,
  menuError,
  activeMenu,
  systemPage,
  systemPageQuery,
  systemPageLoading,
  systemPageError,
  onMenuSelect,
  onSystemPageChange,
  onSystemPageSearch,
  onCreateSystemRecord,
  onUpdateSystemRecord,
  onDeleteSystemRecord,
  onResetUserPassword,
  onLoadMenuPermissionOptions,
  onLoadMenuPermissions,
  onReplaceMenuPermissions,
  onLoadUserRoleOptions,
  onLoadUserRoles,
  onReplaceUserRoles,
  loading,
  error,
  onReload,
  onLogout,
}: DashboardProps) {
  const menuCount = countMenuNodes(menuTree);
  const inSystemPage = isSystemPageMenu(activeMenu?.path);
  const pageTitle = activeMenu?.menuName || 'Manager Dashboard';

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">CF</div>
          <div>
            <strong>Cloud Family</strong>
            <span>Separated Frontend</span>
          </div>
        </div>

        <SidebarMenu nodes={menuTree} error={menuError} activeMenuId={activeMenu?.id} onMenuSelect={onMenuSelect} />
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <h1>{pageTitle}</h1>
            <p>{inSystemPage ? '系统数据维护、分页查询和操作权限配置。' : '登录身份、Gateway 转发和后端响应状态。'}</p>
          </div>
          <div className="topbar-actions">
            <button className="icon-button" type="button" onClick={onReload} disabled={loading} title="刷新">
              <RefreshCw size={18} />
            </button>
            <button className="ghost-button" type="button" onClick={onLogout}>
              <LogOut size={18} />
              退出
            </button>
          </div>
        </header>

        {error ? <div className="error-banner">{error}</div> : null}

        <div className="summary-grid">
          <InfoCard title="当前账号" value={session.username || 'unknown'} meta={session.userType || '未返回用户类型'} />
          <InfoCard title="角色数量" value={String(session.roles.length)} meta={session.roles.join(', ') || '无角色'} />
          <InfoCard title="菜单数量" value={String(menuCount)} meta="当前登录用户可见菜单" />
        </div>

        {inSystemPage ? (
          <SystemPagePanel
            menu={activeMenu}
            page={systemPage}
            query={systemPageQuery}
            loading={systemPageLoading}
            error={systemPageError}
            onPageChange={onSystemPageChange}
            onSearch={onSystemPageSearch}
            onCreate={onCreateSystemRecord}
            onUpdate={onUpdateSystemRecord}
            onDelete={onDeleteSystemRecord}
            onResetPassword={onResetUserPassword}
            onLoadMenuPermissionOptions={onLoadMenuPermissionOptions}
            onLoadMenuPermissions={onLoadMenuPermissions}
            onReplaceMenuPermissions={onReplaceMenuPermissions}
            onLoadUserRoleOptions={onLoadUserRoleOptions}
            onLoadUserRoles={onLoadUserRoles}
            onReplaceUserRoles={onReplaceUserRoles}
          />
        ) : (
          <>
            <MenuTreePanel nodes={menuTree} total={menuCount} error={menuError} />
            <div className="content-grid">
              <DataPanel title="用户服务 /api/users/me" data={userProfile} emptyText="当前账号暂无用户端响应" />
              <DataPanel title="管理服务 /api/manager/dashboard" data={managerDashboard} emptyText="非管理账号可能无法访问该接口" />
            </div>
          </>
        )}
      </section>
    </main>
  );
}

function InfoCard({ title, value, meta }: { title: string; value: string; meta: string }) {
  return (
    <article className="info-card">
      <span>{title}</span>
      <strong>{value}</strong>
      <p>{meta}</p>
    </article>
  );
}

function DataPanel({ title, data, emptyText }: { title: string; data: unknown; emptyText: string }) {
  return (
    <article className="data-panel">
      <h2>{title}</h2>
      {data ? <pre>{JSON.stringify(data, null, 2)}</pre> : <div className="empty-state">{emptyText}</div>}
    </article>
  );
}

function SidebarMenu({
  nodes,
  error,
  activeMenuId,
  onMenuSelect,
}: {
  nodes: MenuTreeNode[];
  error: string | null;
  activeMenuId?: number;
  onMenuSelect: (menu: MenuTreeNode) => void;
}) {
  const [expandedIds, setExpandedIds] = useState<Set<number>>(() => new Set());

  useEffect(() => {
    if (nodes.length > 0 && expandedIds.size === 0) {
      setExpandedIds(new Set(nodes.map((node) => node.id)));
    }
  }, [nodes, expandedIds.size]);

  function toggleNode(node: MenuTreeNode) {
    setExpandedIds((current) => {
      const next = new Set(current);
      if (next.has(node.id)) {
        next.delete(node.id);
      } else {
        next.add(node.id);
      }
      return next;
    });
  }

  return (
    <nav className="nav-list" aria-label="当前用户菜单">
      {error ? (
        <div className="nav-empty">菜单加载失败</div>
      ) : nodes.length > 0 ? (
        nodes.map((node) => (
          <SidebarMenuNode
            key={node.id}
            node={node}
            level={0}
            activeMenuId={activeMenuId}
            expandedIds={expandedIds}
            onToggle={toggleNode}
            onMenuSelect={onMenuSelect}
          />
        ))
      ) : (
        <div className="nav-empty">暂无菜单</div>
      )}
    </nav>
  );
}

function SidebarMenuNode({
  node,
  level,
  activeMenuId,
  expandedIds,
  onToggle,
  onMenuSelect,
}: {
  node: MenuTreeNode;
  level: number;
  activeMenuId?: number;
  expandedIds: Set<number>;
  onToggle: (menu: MenuTreeNode) => void;
  onMenuSelect: (menu: MenuTreeNode) => void;
}) {
  const hasChildren = node.children.length > 0;
  const expanded = expandedIds.has(node.id);

  return (
    <div className="nav-tree-item">
      <button
        className={node.id === activeMenuId ? 'active' : ''}
        type="button"
        style={{ paddingLeft: 12 + level * 18 }}
        aria-expanded={hasChildren ? expanded : undefined}
        onClick={() => (hasChildren ? onToggle(node) : onMenuSelect(node))}
      >
        {hasChildren ? <ChevronRight className={expanded ? 'chevron expanded' : 'chevron'} size={15} /> : <FolderTree size={15} />}
        <span>{node.menuName || node.menuCode || `菜单 ${node.id}`}</span>
      </button>
      {hasChildren && expanded ? (
        <div className="nav-tree-children">
          {node.children.map((child) => (
            <SidebarMenuNode
              key={child.id}
              node={child}
              level={level + 1}
              activeMenuId={activeMenuId}
              expandedIds={expandedIds}
              onToggle={onToggle}
              onMenuSelect={onMenuSelect}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function SystemPagePanel({
  menu,
  page,
  query,
  loading,
  error,
  onPageChange,
  onSearch,
  onCreate,
  onUpdate,
  onDelete,
  onResetPassword,
  onLoadMenuPermissionOptions,
  onLoadMenuPermissions,
  onReplaceMenuPermissions,
  onLoadUserRoleOptions,
  onLoadUserRoles,
  onReplaceUserRoles,
}: {
  menu: MenuTreeNode | null;
  page: PageResult<SystemPageRecord> | null;
  query: PageQuery;
  loading: boolean;
  error: string | null;
  onPageChange: (pageNum: number) => void;
  onSearch: (query: PageQuery) => void;
  onCreate: (payload: SystemRecordPayload) => Promise<void>;
  onUpdate: (id: number, payload: SystemRecordPayload) => Promise<void>;
  onDelete: (id: number) => Promise<void>;
  onResetPassword: (id: number, password: string) => Promise<void>;
  onLoadMenuPermissionOptions: () => Promise<SysPermission[]>;
  onLoadMenuPermissions: (id: number) => Promise<SysPermission[]>;
  onReplaceMenuPermissions: (id: number, ids: number[]) => Promise<void>;
  onLoadUserRoleOptions: () => Promise<SysRole[]>;
  onLoadUserRoles: (id: number) => Promise<SysRole[]>;
  onReplaceUserRoles: (id: number, ids: number[]) => Promise<void>;
}) {
  const config = getSystemPageConfig(menu?.path);
  const isMenuManagement = menu?.path === '/api/manager/system/menus';
  const isOperationLogPage = menu?.path === '/api/manager/system/operation-logs';
  const readOnly = isOperationLogPage || config.fields.length === 0;
  const records = page?.records || [];
  const pageNum = page?.pageNum || 1;
  const pageSize = page?.pageSize || 10;
  const total = page?.total || 0;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const [viewMode, setViewMode] = useState<'table' | 'tree'>('table');
  const [editorRecord, setEditorRecord] = useState<SystemPageRecord | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editorError, setEditorError] = useState<string | null>(null);
  const [passwordRecord, setPasswordRecord] = useState<SystemPageRecord | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [permissionRecord, setPermissionRecord] = useState<SystemPageRecord | null>(null);
  const [permissionOptions, setPermissionOptions] = useState<SysPermission[]>([]);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<Set<number>>(() => new Set());
  const [permissionError, setPermissionError] = useState<string | null>(null);
  const [roleRecord, setRoleRecord] = useState<SystemPageRecord | null>(null);
  const [roleOptions, setRoleOptions] = useState<SysRole[]>([]);
  const [selectedRoleIds, setSelectedRoleIds] = useState<Set<number>>(() => new Set());
  const [roleError, setRoleError] = useState<string | null>(null);
  const [logDetailRecord, setLogDetailRecord] = useState<OperationLog | null>(null);
  const [operationMessage, setOperationMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [treeNodes, setTreeNodes] = useState<MenuTreeNode[]>([]);
  const [menuParentOptions, setMenuParentOptions] = useState<MenuSelectOption[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);
  const [treeError, setTreeError] = useState<string | null>(null);
  const [selectedTreeMenu, setSelectedTreeMenu] = useState<MenuTreeNode | null>(null);

  useEffect(() => {
    setViewMode('table');
    setSelectedTreeMenu(null);
    setTreeNodes([]);
    setTreeError(null);
  }, [menu?.path]);

  useEffect(() => {
    if (isMenuManagement && viewMode === 'tree' && treeNodes.length === 0 && !treeLoading) {
      void reloadMenuTree();
    }
  }, [isMenuManagement, viewMode, treeNodes.length, treeLoading]);

  async function reloadMenuTree() {
    setTreeLoading(true);
    setTreeError(null);
    try {
      const nodes = await loadSystemMenuTree();
      setTreeNodes(nodes);
      setSelectedTreeMenu((current) => {
        if (!current) {
          return nodes[0] || null;
        }
        return findMenuNode(nodes, current.id) || nodes[0] || null;
      });
    } catch (err) {
      setTreeError(readErrorMessage(err));
    } finally {
      setTreeLoading(false);
    }
  }

  function openCreate() {
    if (isMenuManagement) {
      void openMenuEditor(null);
      return;
    }
    setEditorRecord(null);
    setEditorError(null);
    setEditorOpen(true);
  }

  function openEdit(record: SystemPageRecord) {
    if (isMenuManagement) {
      void openMenuEditor(record);
      return;
    }
    setEditorRecord(record);
    setEditorError(null);
    setEditorOpen(true);
  }

  async function openMenuEditor(record: SystemPageRecord | null, initialRecord?: SystemPageRecord | null) {
    setSaving(true);
    setEditorError(null);
    try {
      const nodes = await loadSystemMenuTree();
      setMenuParentOptions(buildMenuParentOptions(nodes, record?.id));
      setTreeNodes(nodes);
      setEditorRecord(initialRecord || record);
      setEditorOpen(true);
    } catch (err) {
      setOperationMessage({ type: 'error', text: readErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function saveRecord(payload: SystemRecordPayload) {
    setSaving(true);
    setEditorError(null);
    try {
      if (editorRecord && editorRecord.id !== 0) {
        await onUpdate(editorRecord.id, payload);
        setOperationMessage({ type: 'success', text: '修改成功' });
      } else {
        await onCreate(payload);
        setOperationMessage({ type: 'success', text: '新增成功' });
      }
      if (isMenuManagement) {
        await reloadMenuTree();
      }
      setEditorOpen(false);
      setEditorRecord(null);
    } catch (err) {
      const message = readErrorMessage(err);
      setEditorError(message);
      setOperationMessage({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  }

  async function deleteRecord(record: SystemPageRecord) {
    if (!window.confirm(`确认删除 ${recordLabel(record)}？`)) {
      return;
    }
    setSaving(true);
    setOperationMessage(null);
    try {
      await onDelete(record.id);
      if (isMenuManagement) {
        await reloadMenuTree();
      }
      setOperationMessage({ type: 'success', text: '删除成功' });
    } catch (err) {
      const message = readErrorMessage(err);
      setEditorError(message);
      setOperationMessage({ type: 'error', text: message });
      setEditorOpen(true);
      setEditorRecord(record);
    } finally {
      setSaving(false);
    }
  }

  async function resetPassword(record: SystemPageRecord, password: string) {
    setSaving(true);
    setPasswordError(null);
    try {
      await onResetPassword(record.id, password);
      setPasswordRecord(null);
      setOperationMessage({ type: 'success', text: '密码重置成功' });
    } catch (err) {
      const message = readErrorMessage(err);
      setPasswordError(message);
      setOperationMessage({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  }

  async function openPermissionDialog(record: SystemPageRecord) {
    setSaving(true);
    setPermissionError(null);
    try {
      const [options, selected] = await Promise.all([onLoadMenuPermissionOptions(), onLoadMenuPermissions(record.id)]);
      setPermissionOptions(options);
      setSelectedPermissionIds(new Set(selected.map((permission) => permission.id)));
      setPermissionRecord(record);
    } catch (err) {
      setOperationMessage({ type: 'error', text: readErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function saveMenuPermissions(ids: number[]) {
    if (!permissionRecord) {
      return;
    }
    setSaving(true);
    setPermissionError(null);
    try {
      await onReplaceMenuPermissions(permissionRecord.id, ids);
      setPermissionRecord(null);
      setOperationMessage({ type: 'success', text: '按钮权限保存成功' });
    } catch (err) {
      const message = readErrorMessage(err);
      setPermissionError(message);
      setOperationMessage({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  }

  async function openRoleDialog(record: SystemPageRecord) {
    setSaving(true);
    setRoleError(null);
    try {
      const [options, selected] = await Promise.all([onLoadUserRoleOptions(), onLoadUserRoles(record.id)]);
      setRoleOptions(options);
      setSelectedRoleIds(new Set(selected.map((role) => role.id)));
      setRoleRecord(record);
    } catch (err) {
      setOperationMessage({ type: 'error', text: readErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function saveUserRoles(ids: number[]) {
    if (!roleRecord) {
      return;
    }
    setSaving(true);
    setRoleError(null);
    try {
      await onReplaceUserRoles(roleRecord.id, ids);
      setRoleRecord(null);
      setOperationMessage({ type: 'success', text: '用户角色保存成功' });
    } catch (err) {
      const message = readErrorMessage(err);
      setRoleError(message);
      setOperationMessage({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  }

  return (
    <article className="table-panel">
      <div className="section-heading">
        <div>
          <h2>{config.title}</h2>
          <p>调用 {config.apiPath} 展示分页数据。</p>
        </div>
        <div className="section-actions">
          {isMenuManagement ? (
            <div className="view-switch" aria-label="菜单展示模式">
              <button type="button" className={viewMode === 'table' ? 'active' : ''} onClick={() => setViewMode('table')}>
                <ListTree size={15} />
                列表
              </button>
              <button type="button" className={viewMode === 'tree' ? 'active' : ''} onClick={() => setViewMode('tree')}>
                <Layers3 size={15} />
                树形配置
              </button>
            </div>
          ) : null}
          {!readOnly ? (
            <button className="primary-button small" type="button" onClick={openCreate} disabled={loading || saving}>
              <Plus size={16} />
              新增
            </button>
          ) : null}
          <div className="pager-actions">
            <button type="button" disabled={loading || pageNum <= 1} onClick={() => onPageChange(pageNum - 1)}>
              上一页
            </button>
            <span>
              {pageNum} / {totalPages}
            </span>
            <button type="button" disabled={loading || pageNum >= totalPages} onClick={() => onPageChange(pageNum + 1)}>
              下一页
            </button>
          </div>
        </div>
      </div>
      {(isMenuManagement && viewMode === 'table') || isOperationLogPage ? (
        <MenuQueryBar
          query={query}
          loading={loading}
          keywordPlaceholder={isOperationLogPage ? '操作者 / 模块 / 类型 / 业务 ID / URI' : '菜单编码 / 名称 / 路径 / 组件'}
          statusLabel={isOperationLogPage ? '操作类型' : '状态'}
          statusOptions={isOperationLogPage
            ? [
                { label: '新增', value: 'CREATE' },
                { label: '修改', value: 'UPDATE' },
                { label: '删除', value: 'DELETE' },
              ]
            : [
                { label: '启用', value: 'ENABLED' },
                { label: '禁用', value: 'DISABLED' },
              ]}
          onSearch={onSearch}
        />
      ) : null}
      {operationMessage ? (
        <div className={operationMessage.type === 'success' ? 'success-banner compact' : 'error-banner compact'}>
          {operationMessage.text}
        </div>
      ) : null}
      {isMenuManagement && viewMode === 'tree' ? (
        <MenuTreeConfigPanel
          nodes={treeNodes}
          selectedMenu={selectedTreeMenu}
          loading={treeLoading}
          error={treeError}
          saving={saving}
          onReload={reloadMenuTree}
          onSelect={setSelectedTreeMenu}
          onCreateChild={(parent) => {
            void openMenuEditor(null, {
              id: 0,
              parentId: parent.id,
              menuCode: '',
              menuName: '',
              path: '',
              component: '',
              icon: '',
              menuLevel: (parent.menuLevel || 1) + 1,
              buttonFlag: false,
              sortOrder: 0,
              visible: true,
              status: 'ENABLED',
            });
          }}
          onEdit={(record) => openEdit(record)}
          onPermissions={(record) => openPermissionDialog(record)}
        />
      ) : error ? (
        <div className="empty-state compact">{error}</div>
      ) : records.length > 0 ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {config.columns.map((column) => (
                  <th key={column.key}>{column.title}</th>
                ))}
                {isOperationLogPage ? <th>详情</th> : null}
                {!readOnly ? <th>操作</th> : null}
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  {config.columns.map((column) => (
                    <td key={column.key}>{column.render(record)}</td>
                  ))}
                  {isOperationLogPage ? (
                    <td>
                      <div className="row-actions">
                        <button type="button" onClick={() => setLogDetailRecord(record as OperationLog)} disabled={saving}>
                          <Search size={14} />
                          详情
                        </button>
                      </div>
                    </td>
                  ) : null}
                  {!readOnly ? (
                    <td>
                      <div className="row-actions">
                        <button type="button" onClick={() => openEdit(record)} disabled={saving}>
                          <Pencil size={14} />
                          修改
                        </button>
                        {menu?.path === '/api/manager/system/users' ? (
                          <>
                            <button type="button" onClick={() => setPasswordRecord(record)} disabled={saving}>
                              <KeyRound size={14} />
                            重置密码
                            </button>
                            <button type="button" onClick={() => openRoleDialog(record)} disabled={saving}>
                              <Users size={14} />
                              分配角色
                            </button>
                          </>
                        ) : null}
                        {menu?.path === '/api/manager/system/menus' ? (
                          <button type="button" onClick={() => openPermissionDialog(record)} disabled={saving}>
                            <KeyRound size={14} />
                            按钮权限
                          </button>
                        ) : null}
                        <button className="danger" type="button" onClick={() => deleteRecord(record)} disabled={saving}>
                          <Trash2 size={14} />
                          删除
                        </button>
                      </div>
                    </td>
                  ) : null}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="empty-state compact">{loading ? '数据加载中。' : '暂无分页数据。'}</div>
      )}
      {editorOpen ? (
        <RecordEditorDialog
          config={withMenuParentOptions(config, menuParentOptions)}
          record={editorRecord?.id === 0 ? null : editorRecord}
          initialRecord={editorRecord?.id === 0 ? editorRecord : null}
          saving={saving}
          error={editorError}
          onClose={() => {
            setEditorOpen(false);
            setEditorRecord(null);
            setEditorError(null);
          }}
          onSave={saveRecord}
        />
      ) : null}
      {passwordRecord ? (
        <PasswordResetDialog
          record={passwordRecord}
          saving={saving}
          error={passwordError}
          onClose={() => {
            setPasswordRecord(null);
            setPasswordError(null);
          }}
          onReset={(password) => resetPassword(passwordRecord, password)}
        />
      ) : null}
      {permissionRecord ? (
        <MenuPermissionDialog
          record={permissionRecord}
          permissions={permissionOptions}
          selectedIds={selectedPermissionIds}
          saving={saving}
          error={permissionError}
          onClose={() => {
            setPermissionRecord(null);
            setPermissionError(null);
          }}
          onSave={saveMenuPermissions}
        />
      ) : null}
      {roleRecord ? (
        <UserRoleDialog
          record={roleRecord}
          roles={roleOptions}
          selectedIds={selectedRoleIds}
          saving={saving}
          error={roleError}
          onClose={() => {
            setRoleRecord(null);
            setRoleError(null);
          }}
          onSave={saveUserRoles}
        />
      ) : null}
      {logDetailRecord ? (
        <OperationLogDetailDialog
          record={logDetailRecord}
          onClose={() => setLogDetailRecord(null)}
        />
      ) : null}
    </article>
  );
}

function MenuTreeConfigPanel({
  nodes,
  selectedMenu,
  loading,
  error,
  saving,
  onReload,
  onSelect,
  onCreateChild,
  onEdit,
  onPermissions,
}: {
  nodes: MenuTreeNode[];
  selectedMenu: MenuTreeNode | null;
  loading: boolean;
  error: string | null;
  saving: boolean;
  onReload: () => Promise<void>;
  onSelect: (menu: MenuTreeNode) => void;
  onCreateChild: (menu: MenuTreeNode) => void;
  onEdit: (menu: MenuTreeNode) => void;
  onPermissions: (menu: MenuTreeNode) => void;
}) {
  const total = countAllMenuNodes(nodes);
  const visibleTotal = countMenuNodes(nodes);
  const [expandedIds, setExpandedIds] = useState<Set<number>>(() => new Set());

  useEffect(() => {
    if (nodes.length === 0) {
      setExpandedIds(new Set());
      return;
    }

    setExpandedIds((current) => {
      const next = new Set(current);
      nodes.forEach((node) => {
        if (node.children.length > 0) {
          next.add(node.id);
        }
      });
      return next;
    });
  }, [nodes]);

  function toggleNode(menu: MenuTreeNode) {
    setExpandedIds((current) => {
      const next = new Set(current);
      if (next.has(menu.id)) {
        next.delete(menu.id);
      } else {
        next.add(menu.id);
      }
      return next;
    });
  }

  return (
    <div className="menu-config">
      <div className="menu-config-tree">
        <div className="subsection-heading">
          <div>
            <h3>菜单树</h3>
            <p>{loading ? '加载中' : `${total} 个菜单，${visibleTotal} 个显示`}</p>
          </div>
          <div className="tree-actions">
            <button className="ghost-button small" type="button" onClick={() => setExpandedIds(new Set(collectExpandableMenuIds(nodes)))} disabled={loading || saving || nodes.length === 0}>
              展开
            </button>
            <button className="ghost-button small" type="button" onClick={() => setExpandedIds(new Set())} disabled={loading || saving || nodes.length === 0}>
              收起
            </button>
            <button className="icon-button" type="button" onClick={() => void onReload()} disabled={loading || saving} title="刷新菜单树">
              <RefreshCw size={16} />
            </button>
          </div>
        </div>
        {error ? (
          <div className="empty-state compact">{error}</div>
        ) : nodes.length > 0 ? (
          <div className="config-tree-list" role="tree">
            {nodes.map((node) => (
              <ConfigMenuNode
                key={node.id}
                node={node}
                level={0}
                selectedId={selectedMenu?.id}
                expandedIds={expandedIds}
                onSelect={onSelect}
                onToggle={toggleNode}
              />
            ))}
          </div>
        ) : (
          <div className="empty-state compact">{loading ? '菜单树加载中。' : '暂无菜单数据。'}</div>
        )}
      </div>

      <div className="menu-config-detail">
        {selectedMenu ? (
          <>
            <div className="detail-title">
              <div>
                <h3>{selectedMenu.menuName || selectedMenu.menuCode || `菜单 ${selectedMenu.id}`}</h3>
                <p>ID {selectedMenu.id}</p>
              </div>
              <div className="detail-actions">
                <button className="ghost-button small" type="button" onClick={() => onCreateChild(selectedMenu)} disabled={saving}>
                  <Plus size={15} />
                  子菜单
                </button>
                <button className="ghost-button small" type="button" onClick={() => onEdit(selectedMenu)} disabled={saving}>
                  <Pencil size={15} />
                  修改
                </button>
                <button className="ghost-button small" type="button" onClick={() => onPermissions(selectedMenu)} disabled={saving}>
                  <KeyRound size={15} />
                  权限
                </button>
              </div>
            </div>
            <div className="menu-detail-grid">
              <DetailItem label="父菜单 ID" value={selectedMenu.parentId ?? '顶级菜单'} />
              <DetailItem label="菜单级别" value={selectedMenu.menuLevel ?? '-'} />
              <DetailItem label="按钮标识" value={selectedMenu.buttonFlag ? '按钮' : '菜单'} />
              <DetailItem label="菜单编码" value={selectedMenu.menuCode} />
              <DetailItem label="访问路径" value={selectedMenu.path} />
              <DetailItem label="组件" value={selectedMenu.component} />
              <DetailItem label="图标" value={selectedMenu.icon} />
              <DetailItem label="排序" value={selectedMenu.sortOrder ?? 0} />
              <DetailItem label="显示状态" value={selectedMenu.visible === false ? '隐藏' : '显示'} />
              <DetailItem label="启用状态" value={selectedMenu.status || '-'} />
            </div>
          </>
        ) : (
          <div className="empty-state compact">请选择一个菜单节点。</div>
        )}
      </div>
    </div>
  );
}

function ConfigMenuNode({
  node,
  level,
  selectedId,
  expandedIds,
  onSelect,
  onToggle,
}: {
  node: MenuTreeNode;
  level: number;
  selectedId?: number;
  expandedIds: Set<number>;
  onSelect: (menu: MenuTreeNode) => void;
  onToggle: (menu: MenuTreeNode) => void;
}) {
  const hasChildren = node.children.length > 0;
  const expanded = expandedIds.has(node.id);

  return (
    <div role="treeitem" aria-expanded={hasChildren ? expanded : undefined}>
      <button
        className={node.id === selectedId ? 'config-tree-node active' : 'config-tree-node'}
        type="button"
        style={{ paddingLeft: 12 + level * 22 }}
        onClick={() => onSelect(node)}
      >
        <span
          className={hasChildren ? `node-toggle visible ${expanded ? 'expanded' : ''}` : 'node-toggle'}
          onClick={(event) => {
            event.stopPropagation();
            if (hasChildren) {
              onToggle(node);
            }
          }}
        >
          <ChevronRight size={15} />
        </span>
        <span className="config-tree-main">
          <strong>{node.menuName || node.menuCode || `菜单 ${node.id}`}</strong>
          <small>{node.path || node.component || '未配置路径'}</small>
        </span>
        <span className="config-tree-state" title={node.visible === false ? '隐藏' : '显示'}>
          {node.visible === false ? <EyeOff size={14} /> : <Eye size={14} />}
        </span>
      </button>
      {hasChildren && expanded ? (
        <div role="group">
          {node.children.map((child) => (
            <ConfigMenuNode
              key={child.id}
              node={child}
              level={level + 1}
              selectedId={selectedId}
              expandedIds={expandedIds}
              onSelect={onSelect}
              onToggle={onToggle}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function DetailItem({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong>{value === null || value === undefined || value === '' ? '-' : value}</strong>
    </div>
  );
}

function MenuQueryBar({
  query,
  loading,
  keywordPlaceholder,
  statusLabel,
  statusOptions,
  onSearch,
}: {
  query: PageQuery;
  loading: boolean;
  keywordPlaceholder: string;
  statusLabel: string;
  statusOptions: Array<{ label: string; value: string }>;
  onSearch: (query: PageQuery) => void;
}) {
  const [keyword, setKeyword] = useState(query.keyword || '');
  const [status, setStatus] = useState(query.status || '');

  useEffect(() => {
    setKeyword(query.keyword || '');
    setStatus(query.status || '');
  }, [query.keyword, query.status]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSearch({
      keyword: keyword.trim() || undefined,
      status: status || undefined,
    });
  }

  function reset() {
    setKeyword('');
    setStatus('');
    onSearch({});
  }

  return (
    <form className="query-bar" onSubmit={submit}>
      <label>
        关键词
        <span className="query-input">
          <Search size={16} />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder={keywordPlaceholder}
          />
        </span>
      </label>
      <label>
        {statusLabel}
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">全部{statusLabel}</option>
          {statusOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      <div className="query-actions">
        <button className="primary-button small" type="submit" disabled={loading}>
          <Search size={16} />
          查询
        </button>
        <button className="ghost-button small" type="button" onClick={reset} disabled={loading && !keyword && !status}>
          <X size={16} />
          重置
        </button>
      </div>
    </form>
  );
}

type TableColumn = {
  key: string;
  title: string;
  render: (record: SystemPageRecord) => ReactNode;
};

type EditorField = {
  key: string;
  label: string;
  type?: 'text' | 'password' | 'number' | 'select' | 'checkbox' | 'textarea';
  valueType?: 'string' | 'number';
  required?: boolean;
  createOnly?: boolean;
  readonlyOnEdit?: boolean;
  options?: Array<{ label: string; value: string }>;
  placeholder?: string;
};

type MenuSelectOption = {
  id: number;
  label: string;
};

type SystemPageConfig = {
  title: string;
  apiPath: string;
  columns: TableColumn[];
  fields: EditorField[];
};

function RecordEditorDialog({
  config,
  record,
  initialRecord,
  saving,
  error,
  onClose,
  onSave,
}: {
  config: SystemPageConfig;
  record: SystemPageRecord | null;
  initialRecord?: SystemPageRecord | null;
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onSave: (payload: SystemRecordPayload) => Promise<void>;
}) {
  const fields = config.fields.filter((field) => !field.createOnly || !record);
  const [values, setValues] = useState<Record<string, string | boolean>>(() => initialEditorValues(fields, initialRecord || record));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void onSave(buildPayload(fields, values));
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="record-dialog" role="dialog" aria-modal="true" aria-label={record ? '修改记录' : '新增记录'}>
        <div className="dialog-heading">
          <div>
            <h3>{record ? `修改${config.title}` : `新增${config.title}`}</h3>
            <p>{record ? `ID ${record.id}` : '填写基础字段后保存。'}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={saving} title="关闭">
            <X size={18} />
          </button>
        </div>
        <form className="record-form" onSubmit={submit}>
          {fields.map((field) => (
            <label key={field.key} className={field.type === 'checkbox' ? 'checkbox-field' : ''}>
              <span>{field.label}</span>
              {renderEditorControl(
                field,
                values[field.key],
                (value) => setValues((current) => ({ ...current, [field.key]: value })),
                Boolean(record && field.readonlyOnEdit),
              )}
            </label>
          ))}
          {error ? <div className="error-banner compact">{error}</div> : null}
          <div className="dialog-actions">
            <button className="ghost-button" type="button" onClick={onClose} disabled={saving}>
              取消
            </button>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? '保存中' : '保存'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function PasswordResetDialog({
  record,
  saving,
  error,
  onClose,
  onReset,
}: {
  record: SystemPageRecord;
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onReset: (password: string) => void;
}) {
  const [password, setPassword] = useState('');

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onReset(password);
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="record-dialog" role="dialog" aria-modal="true" aria-label="重置密码">
        <div className="dialog-heading">
          <div>
            <h3>重置密码</h3>
            <p>{recordLabel(record)}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={saving} title="关闭">
            <X size={18} />
          </button>
        </div>
        <form className="record-form single-column" onSubmit={submit}>
          <label>
            <span>新密码</span>
            <input
              autoComplete="new-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>
          {error ? <div className="error-banner compact">{error}</div> : null}
          <div className="dialog-actions">
            <button className="ghost-button" type="button" onClick={onClose} disabled={saving}>
              取消
            </button>
            <button className="primary-button" type="submit" disabled={saving || !password.trim()}>
              {saving ? '重置中' : '重置'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function MenuPermissionDialog({
  record,
  permissions,
  selectedIds,
  saving,
  error,
  onClose,
  onSave,
}: {
  record: SystemPageRecord;
  permissions: SysPermission[];
  selectedIds: Set<number>;
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onSave: (ids: number[]) => void;
}) {
  const [nextIds, setNextIds] = useState<Set<number>>(() => new Set(selectedIds));

  function toggle(permissionId: number) {
    setNextIds((current) => {
      const next = new Set(current);
      if (next.has(permissionId)) {
        next.delete(permissionId);
      } else {
        next.add(permissionId);
      }
      return next;
    });
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(Array.from(nextIds));
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="record-dialog" role="dialog" aria-modal="true" aria-label="按钮权限">
        <div className="dialog-heading">
          <div>
            <h3>按钮权限</h3>
            <p>{recordLabel(record)}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={saving} title="关闭">
            <X size={18} />
          </button>
        </div>
        <form className="record-form single-column" onSubmit={submit}>
          <div className="permission-list">
            {permissions.length > 0 ? (
              permissions.map((permission) => (
                <label key={permission.id} className="permission-option">
                  <input
                    type="checkbox"
                    checked={nextIds.has(permission.id)}
                    onChange={() => toggle(permission.id)}
                    disabled={saving}
                  />
                  <span>
                    <strong>{permission.permissionName || permission.permissionCode}</strong>
                    <small>{permission.permissionCode}</small>
                  </span>
                </label>
              ))
            ) : (
              <div className="empty-state compact">暂无可选权限。</div>
            )}
          </div>
          {error ? <div className="error-banner compact">{error}</div> : null}
          <div className="dialog-actions">
            <button className="ghost-button" type="button" onClick={onClose} disabled={saving}>
              取消
            </button>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? '保存中' : '保存'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function UserRoleDialog({
  record,
  roles,
  selectedIds,
  saving,
  error,
  onClose,
  onSave,
}: {
  record: SystemPageRecord;
  roles: SysRole[];
  selectedIds: Set<number>;
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onSave: (ids: number[]) => void;
}) {
  const [nextIds, setNextIds] = useState<Set<number>>(() => new Set(selectedIds));

  function toggle(roleId: number) {
    setNextIds((current) => {
      const next = new Set(current);
      if (next.has(roleId)) {
        next.delete(roleId);
      } else {
        next.add(roleId);
      }
      return next;
    });
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(Array.from(nextIds));
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="record-dialog" role="dialog" aria-modal="true" aria-label="分配角色">
        <div className="dialog-heading">
          <div>
            <h3>分配角色</h3>
            <p>{recordLabel(record)}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={saving} title="关闭">
            <X size={18} />
          </button>
        </div>
        <form className="record-form single-column" onSubmit={submit}>
          <div className="permission-list">
            {roles.length > 0 ? (
              roles.map((role) => (
                <label key={role.id} className="permission-option">
                  <input
                    type="checkbox"
                    checked={nextIds.has(role.id)}
                    onChange={() => toggle(role.id)}
                    disabled={saving}
                  />
                  <span>
                    <strong>{role.roleName || role.roleCode}</strong>
                    <small>{role.roleCode}</small>
                  </span>
                </label>
              ))
            ) : (
              <div className="empty-state compact">暂无可选角色。</div>
            )}
          </div>
          {error ? <div className="error-banner compact">{error}</div> : null}
          <div className="dialog-actions">
            <button className="ghost-button" type="button" onClick={onClose} disabled={saving}>
              取消
            </button>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? '保存中' : '保存'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function OperationLogDetailDialog({ record, onClose }: { record: OperationLog; onClose: () => void }) {
  const before = parseLogData(record.beforeData);
  const after = parseLogData(record.afterData);
  const rows = buildLogCompareRows(before, after);

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="record-dialog log-detail-dialog" role="dialog" aria-modal="true" aria-label="操作日志详情">
        <div className="dialog-heading">
          <div>
            <h3>操作日志详情</h3>
            <p>ID {record.id} · {formatDate(record.operationAt)}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} title="关闭">
            <X size={18} />
          </button>
        </div>
        <div className="log-detail-body">
          <div className="log-meta-grid">
            <DetailItem label="操作类型" value={operationTypeLabel(record.operationType)} />
            <DetailItem label="操作者" value={record.operatorUsername} />
            <DetailItem label="用户类型" value={record.operatorUserType} />
            <DetailItem label="业务模块" value={record.businessModule} />
            <DetailItem label="业务类型" value={record.businessType} />
            <DetailItem label="业务 ID" value={record.businessId} />
            <DetailItem label="业务名称" value={record.businessName} />
            <DetailItem label="请求方法" value={record.requestMethod} />
            <DetailItem label="请求 URI" value={record.requestUri} />
            <DetailItem label="客户端 IP" value={record.clientIp} />
          </div>

          <div className="compare-table-wrap">
            <table className="compare-table">
              <thead>
                <tr>
                  <th>字段</th>
                  <th>变更前</th>
                  <th>变更后</th>
                </tr>
              </thead>
              <tbody>
                {rows.length > 0 ? rows.map((row) => (
                  <tr key={row.key} className={row.changed ? 'changed' : ''}>
                    <td>{row.key}</td>
                    <td><code>{row.beforeValue}</code></td>
                    <td><code>{row.afterValue}</code></td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan={3}>暂无可对比数据。</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="dialog-actions">
            <button className="primary-button" type="button" onClick={onClose}>
              关闭
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

function renderEditorControl(
  field: EditorField,
  value: string | boolean | undefined,
  onChange: (value: string | boolean) => void,
  disabled = false,
) {
  if (field.type === 'select') {
    return (
      <select value={String(value ?? '')} onChange={(event) => onChange(event.target.value)} required={field.required} disabled={disabled}>
        {field.placeholder ? <option value="">{field.placeholder}</option> : null}
        {field.options?.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    );
  }

  if (field.type === 'textarea') {
    return (
      <textarea
        value={String(value ?? '')}
        onChange={(event) => onChange(event.target.value)}
        placeholder={field.placeholder}
        required={field.required}
        disabled={disabled}
      />
    );
  }

  if (field.type === 'checkbox') {
    return <input type="checkbox" checked={Boolean(value)} onChange={(event) => onChange(event.target.checked)} disabled={disabled} />;
  }

  return (
    <input
      type={field.type || 'text'}
      value={String(value ?? '')}
      onChange={(event) => onChange(event.target.value)}
      placeholder={field.placeholder}
      required={field.required}
      disabled={disabled}
    />
  );
}

function initialEditorValues(fields: EditorField[], record: SystemPageRecord | null) {
  const values: Record<string, string | boolean> = {};
  fields.forEach((field) => {
    const raw = record ? (record as Record<string, unknown>)[field.key] : undefined;
    if (field.type === 'checkbox') {
      values[field.key] = raw === undefined ? true : Boolean(raw);
    } else if (field.key === 'status') {
      values[field.key] = typeof raw === 'string' ? raw : 'ENABLED';
    } else {
      values[field.key] = raw === null || raw === undefined ? '' : String(raw);
    }
  });
  return values;
}

function buildPayload(fields: EditorField[], values: Record<string, string | boolean>) {
  const payload: SystemRecordPayload = {};
  fields.forEach((field) => {
    const value = values[field.key];
    if (field.type === 'checkbox') {
      payload[field.key] = Boolean(value);
      return;
    }
    if (field.type === 'number') {
      payload[field.key] = value === '' || value === undefined ? null : Number(value);
      return;
    }
    if (field.valueType === 'number') {
      payload[field.key] = value === '' || value === undefined ? null : Number(value);
      return;
    }
    const text = typeof value === 'string' ? value.trim() : '';
    payload[field.key] = text || undefined;
  });
  return payload;
}

function withMenuParentOptions(config: SystemPageConfig, options: MenuSelectOption[]) {
  if (config.apiPath !== '/api/manager/system/menus/page') {
    return config;
  }
  return {
    ...config,
    fields: config.fields.map((field) => {
      if (field.key !== 'parentId') {
        return field;
      }
      return {
        ...field,
        type: 'select' as const,
        valueType: 'number' as const,
        placeholder: '顶级菜单',
        options: options.map((option) => ({
          label: option.label,
          value: String(option.id),
        })),
      };
    }),
  };
}

function getSystemPageConfig(path?: string): SystemPageConfig {
  const statusField: EditorField = {
    key: 'status',
    label: '状态',
    type: 'select',
    required: true,
    options: [
      { label: '启用', value: 'ENABLED' },
      { label: '禁用', value: 'DISABLED' },
    ],
  };
  const commonColumns: TableColumn[] = [
    { key: 'id', title: 'ID', render: (record) => record.id },
    { key: 'status', title: '状态', render: (record) => <StatusValue value={readField(record, 'status')} /> },
    { key: 'createdAt', title: '创建时间', render: (record) => formatDate(readField(record, 'createdAt')) },
  ];

  if (path === '/api/manager/system/roles') {
    return {
      title: '角色管理',
      apiPath: '/api/manager/system/roles/page',
      columns: [
        commonColumns[0],
        { key: 'roleCode', title: '角色编码', render: (record) => textValue(readField(record, 'roleCode')) },
        { key: 'roleName', title: '角色名称', render: (record) => textValue(readField(record, 'roleName')) },
        { key: 'description', title: '描述', render: (record) => textValue(readField(record, 'description')) },
        commonColumns[1],
        commonColumns[2],
      ],
      fields: [
        { key: 'roleCode', label: '角色编码', required: true, placeholder: '例如 SUPER_ADMIN' },
        { key: 'roleName', label: '角色名称', required: true },
        { key: 'description', label: '描述', type: 'textarea' },
        statusField,
      ],
    };
  }

  if (path === '/api/manager/system/permissions') {
    return {
      title: '权限管理',
      apiPath: '/api/manager/system/permissions/page',
      columns: [
        commonColumns[0],
        { key: 'permissionCode', title: '权限编码', render: (record) => textValue(readField(record, 'permissionCode')) },
        { key: 'permissionName', title: '权限名称', render: (record) => textValue(readField(record, 'permissionName')) },
        { key: 'description', title: '描述', render: (record) => textValue(readField(record, 'description')) },
        commonColumns[1],
        commonColumns[2],
      ],
      fields: [
        { key: 'permissionCode', label: '权限编码', required: true, placeholder: '例如 menu:read' },
        { key: 'permissionName', label: '权限名称', required: true },
        { key: 'description', label: '描述', type: 'textarea' },
        statusField,
      ],
    };
  }

  if (path === '/api/manager/system/menus') {
    return {
      title: '菜单管理',
      apiPath: '/api/manager/system/menus/page',
      columns: [
        commonColumns[0],
        { key: 'menuCode', title: '菜单编码', render: (record) => textValue(readField(record, 'menuCode')) },
        { key: 'menuName', title: '菜单名称', render: (record) => textValue(readField(record, 'menuName')) },
        { key: 'menuLevel', title: '级别', render: (record) => textValue(readField(record, 'menuLevel')) },
        { key: 'buttonFlag', title: '标识', render: (record) => <span className={readBooleanField(record, 'buttonFlag') ? 'muted-pill' : 'status-pill'}>{readBooleanField(record, 'buttonFlag') ? '按钮' : '菜单'}</span> },
        { key: 'path', title: '路径', render: (record) => textValue(readField(record, 'path')) },
        { key: 'component', title: '组件', render: (record) => textValue(readField(record, 'component')) },
        commonColumns[1],
        commonColumns[2],
      ],
      fields: [
        { key: 'parentId', label: '父菜单', type: 'select', valueType: 'number', placeholder: '顶级菜单' },
        { key: 'menuCode', label: '菜单编码', required: true },
        { key: 'menuName', label: '菜单名称', required: true },
        { key: 'path', label: '路径', required: true },
        { key: 'component', label: '组件', required: true },
        { key: 'icon', label: '图标' },
        { key: 'buttonFlag', label: '按钮标识', type: 'checkbox' },
        { key: 'sortOrder', label: '排序', type: 'number' },
        { key: 'visible', label: '显示', type: 'checkbox' },
        statusField,
      ],
    };
  }

  if (path === '/api/manager/system/operation-logs') {
    return {
      title: '操作日志',
      apiPath: '/api/manager/system/operation-logs/page',
      columns: [
        commonColumns[0],
        { key: 'operationType', title: '操作', render: (record) => <OperationTypeValue value={readField(record, 'operationType')} /> },
        { key: 'operatorUsername', title: '操作者', render: (record) => textValue(readField(record, 'operatorUsername')) },
        { key: 'businessModule', title: '模块', render: (record) => textValue(readField(record, 'businessModule')) },
        { key: 'businessType', title: '业务类型', render: (record) => textValue(readField(record, 'businessType')) },
        { key: 'businessName', title: '业务名称', render: (record) => textValue(readField(record, 'businessName')) },
        { key: 'requestUri', title: '请求 URI', render: (record) => textValue(readField(record, 'requestUri')) },
        { key: 'operationAt', title: '操作时间', render: (record) => formatDate(readField(record, 'operationAt')) },
      ],
      fields: [],
    };
  }

  return {
    title: '用户管理',
    apiPath: '/api/manager/system/users/page',
    columns: [
      commonColumns[0],
      { key: 'username', title: '账号', render: (record) => textValue(readField(record, 'username')) },
      { key: 'displayName', title: '显示名称', render: (record) => textValue(readField(record, 'displayName')) },
      { key: 'email', title: '邮箱', render: (record) => textValue(readField(record, 'email')) },
      commonColumns[1],
      commonColumns[2],
    ],
    fields: [
      { key: 'username', label: '账号', required: true, readonlyOnEdit: true },
      { key: 'password', label: '密码', type: 'password', createOnly: true, required: true },
      { key: 'displayName', label: '显示名称' },
      { key: 'email', label: '邮箱' },
      statusField,
    ],
  };
}

function isSystemPageMenu(path?: string) {
  return path === '/api/manager/system/users'
    || path === '/api/manager/system/roles'
    || path === '/api/manager/system/permissions'
    || path === '/api/manager/system/menus'
    || path === '/api/manager/system/operation-logs';
}

function StatusValue({ value }: { value?: string }) {
  return <span className={value === 'ENABLED' ? 'status-pill' : 'muted-pill'}>{value || '-'}</span>;
}

function OperationTypeValue({ value }: { value?: string }) {
  return <span className={value === 'DELETE' ? 'danger-pill' : 'status-pill'}>{operationTypeLabel(value)}</span>;
}

function operationTypeLabel(value?: string) {
  const labelMap: Record<string, string> = {
    CREATE: '新增',
    UPDATE: '修改',
    DELETE: '删除',
  };
  return value ? labelMap[value] || value : '-';
}

function readField(record: SystemPageRecord, key: string) {
  const value = (record as Record<string, unknown>)[key];
  return typeof value === 'string' || typeof value === 'number' ? String(value) : undefined;
}

function readBooleanField(record: SystemPageRecord, key: string) {
  return Boolean((record as Record<string, unknown>)[key]);
}

type ParsedLogData = {
  flattened: Record<string, string>;
  raw: string;
};

type CompareRow = {
  key: string;
  beforeValue: string;
  afterValue: string;
  changed: boolean;
};

function parseLogData(value?: string): ParsedLogData {
  if (!value || !value.trim()) {
    return { flattened: {}, raw: '' };
  }
  try {
    const parsed = JSON.parse(value) as unknown;
    return {
      flattened: flattenLogValue(parsed),
      raw: formatLogValue(parsed),
    };
  } catch {
    return {
      flattened: { 原始数据: value },
      raw: value,
    };
  }
}

function flattenLogValue(value: unknown, prefix = ''): Record<string, string> {
  if (value === null || value === undefined) {
    return prefix ? { [prefix]: '-' } : {};
  }
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return prefix ? { [prefix]: '[]' } : {};
    }
    return value.reduce<Record<string, string>>((result, item, index) => ({
      ...result,
      ...flattenLogValue(item, prefix ? `${prefix}[${index}]` : `[${index}]`),
    }), {});
  }
  if (typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>);
    if (entries.length === 0) {
      return prefix ? { [prefix]: '{}' } : {};
    }
    return entries.reduce<Record<string, string>>((result, [key, item]) => ({
      ...result,
      ...flattenLogValue(item, prefix ? `${prefix}.${key}` : key),
    }), {});
  }
  return prefix ? { [prefix]: formatLogValue(value) } : { 值: formatLogValue(value) };
}

function buildLogCompareRows(before: ParsedLogData, after: ParsedLogData): CompareRow[] {
  const keys = Array.from(new Set([...Object.keys(before.flattened), ...Object.keys(after.flattened)])).sort();
  if (keys.length === 0 && (before.raw || after.raw)) {
    keys.push('原始数据');
  }
  return keys.map((key) => {
    const beforeValue = before.flattened[key] ?? (key === '原始数据' ? before.raw : '-');
    const afterValue = after.flattened[key] ?? (key === '原始数据' ? after.raw : '-');
    return {
      key,
      beforeValue,
      afterValue,
      changed: beforeValue !== afterValue,
    };
  });
}

function formatLogValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return JSON.stringify(value, null, 2);
}

function recordLabel(record: SystemPageRecord) {
  return textValue(
    readField(record, 'username')
      || readField(record, 'roleName')
      || readField(record, 'permissionName')
      || readField(record, 'menuName')
      || String(record.id),
  );
}

function readErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return '操作失败';
}

function MenuTreePanel({ nodes, total, error }: { nodes: MenuTreeNode[]; total: number; error: string | null }) {
  return (
    <article className="menu-tree-panel">
      <div className="section-heading">
        <div>
          <h2>当前用户菜单树</h2>
          <p>调用 /api/manager/system/menus/current-user/tree，按登录用户角色授权展示 {total} 个可见菜单。</p>
        </div>
      </div>
      {error ? (
        <div className="empty-state compact">{error}</div>
      ) : nodes.length > 0 ? (
        <div className="menu-tree" role="tree">
          {nodes.map((node) => (
            <MenuNode key={node.id} node={node} level={0} />
          ))}
        </div>
      ) : (
        <div className="empty-state compact">当前登录用户暂无可显示菜单。</div>
      )}
    </article>
  );
}

function MenuNode({ node, level }: { node: MenuTreeNode; level: number }) {
  const hasChildren = node.children.length > 0;

  return (
    <div className="menu-node" role="treeitem" aria-expanded={hasChildren || undefined}>
      <div className="menu-node-row" style={{ paddingLeft: 14 + level * 22 }}>
        <span className={hasChildren ? 'node-toggle visible' : 'node-toggle'}>
          <ChevronRight size={15} />
        </span>
        <div className="menu-node-main">
          <strong>{node.menuName || node.menuCode || `菜单 ${node.id}`}</strong>
          <span>{node.path || node.component || '未配置路径'}</span>
        </div>
        <div className="menu-node-meta">
          <span>{node.menuCode || '-'}</span>
          <span className={node.visible === false ? 'muted-pill' : 'status-pill'}>{node.visible === false ? '隐藏' : '显示'}</span>
        </div>
      </div>
      {hasChildren ? (
        <div role="group">
          {node.children.map((child) => (
            <MenuNode key={child.id} node={child} level={level + 1} />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function countMenuNodes(nodes: MenuTreeNode[]): number {
  return nodes.reduce((total, node) => {
    const self = node.visible === false ? 0 : 1;
    return total + self + countMenuNodes(node.children || []);
  }, 0);
}

function countAllMenuNodes(nodes: MenuTreeNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countAllMenuNodes(node.children || []), 0);
}

function findMenuNode(nodes: MenuTreeNode[], id: number): MenuTreeNode | null {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    const child = findMenuNode(node.children || [], id);
    if (child) {
      return child;
    }
  }
  return null;
}

function collectExpandableMenuIds(nodes: MenuTreeNode[]): number[] {
  return nodes.flatMap((node) => {
    if (node.children.length === 0) {
      return [];
    }
    return [node.id, ...collectExpandableMenuIds(node.children)];
  });
}

function buildMenuParentOptions(nodes: MenuTreeNode[], excludedId?: number): MenuSelectOption[] {
  const excludedIds = excludedId ? collectMenuSubtreeIds(nodes, excludedId) : new Set<number>();
  return flattenMenuOptions(nodes, excludedIds);
}

function collectMenuSubtreeIds(nodes: MenuTreeNode[], id: number): Set<number> {
  const target = findMenuNode(nodes, id);
  if (!target) {
    return new Set([id]);
  }
  return new Set([id, ...collectAllChildMenuIds(target.children || [])]);
}

function collectAllChildMenuIds(nodes: MenuTreeNode[]): number[] {
  return nodes.flatMap((node) => [node.id, ...collectAllChildMenuIds(node.children || [])]);
}

function flattenMenuOptions(nodes: MenuTreeNode[], excludedIds: Set<number>, level = 0): MenuSelectOption[] {
  return nodes.flatMap((node) => {
    const children = flattenMenuOptions(node.children || [], excludedIds, level + 1);
    if (excludedIds.has(node.id) || node.buttonFlag) {
      return children;
    }
    return [
      {
        id: node.id,
        label: `${'  '.repeat(level)}${node.menuName || node.menuCode || `菜单 ${node.id}`} (${node.menuCode || node.id})`,
      },
      ...children,
    ];
  });
}

function formatDate(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function textValue(value?: string) {
  return value && value.trim() ? value : '-';
}
