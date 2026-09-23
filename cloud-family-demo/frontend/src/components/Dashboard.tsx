import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { ChevronRight, Eye, EyeOff, FolderTree, KeyRound, Layers3, ListTree, LogOut, Pencil, Plus, RefreshCw, Search, Trash2, Users, X } from 'lucide-react';
import type { ManagerDashboard, MenuTreeNode, OperationLog, PageQuery, PageResult, SysMenu, SysPermission, SysRole, SystemPageRecord, SystemRecordPayload, UserProfile } from '../types/api';
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
  onLoadRoleMenus: (id: number) => Promise<SysMenu[]>;
  onReplaceRoleMenus: (id: number, ids: number[]) => Promise<void>;
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
  onLoadRoleMenus,
  onReplaceRoleMenus,
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
            onLoadRoleMenus={onLoadRoleMenus}
            onReplaceRoleMenus={onReplaceRoleMenus}
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
  onLoadRoleMenus,
  onReplaceRoleMenus,
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
  onLoadRoleMenus: (id: number) => Promise<SysMenu[]>;
  onReplaceRoleMenus: (id: number, ids: number[]) => Promise<void>;
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
  const [permissionMenuTree, setPermissionMenuTree] = useState<MenuTreeNode[]>([]);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<Set<number>>(() => new Set());
  const [permissionError, setPermissionError] = useState<string | null>(null);
  const [roleRecord, setRoleRecord] = useState<SystemPageRecord | null>(null);
  const [roleOptions, setRoleOptions] = useState<SysRole[]>([]);
  const [selectedRoleIds, setSelectedRoleIds] = useState<Set<number>>(() => new Set());
  const [roleError, setRoleError] = useState<string | null>(null);
  const [roleMenuRecord, setRoleMenuRecord] = useState<SystemPageRecord | null>(null);
  const [roleMenuOptions, setRoleMenuOptions] = useState<MenuTreeNode[]>([]);
  const [selectedRoleMenuIds, setSelectedRoleMenuIds] = useState<Set<number>>(() => new Set());
  const [roleMenuError, setRoleMenuError] = useState<string | null>(null);
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
      if (isOperationLogMenuRecord(record)) {
        setOperationMessage({ type: 'error', text: '操作日志菜单仅允许查询，不允许修改' });
        return;
      }
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
    if (isMenuManagement && isOperationLogMenuRecord(record)) {
      setOperationMessage({ type: 'error', text: '操作日志菜单仅允许查询，不允许修改' });
      return;
    }
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
    if (isMenuManagement && isOperationLogMenuRecord(record)) {
      setOperationMessage({ type: 'error', text: '操作日志菜单仅允许查询，不允许修改' });
      return;
    }
    setSaving(true);
    setPermissionError(null);
    try {
      const [options, selected, nodes] = await Promise.all([
        onLoadMenuPermissionOptions(),
        onLoadMenuPermissions(record.id),
        loadSystemMenuTree(),
      ]);
      setPermissionOptions(options);
      setPermissionMenuTree(nodes);
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

  async function openRoleMenuDialog(record: SystemPageRecord) {
    setSaving(true);
    setRoleMenuError(null);
    try {
      const [nodes, selected] = await Promise.all([loadSystemMenuTree(), onLoadRoleMenus(record.id)]);
      setRoleMenuOptions(nodes);
      setSelectedRoleMenuIds(new Set(selected.map((menuItem) => menuItem.id)));
      setRoleMenuRecord(record);
    } catch (err) {
      setOperationMessage({ type: 'error', text: readErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function saveRoleMenus(ids: number[]) {
    if (!roleMenuRecord) {
      return;
    }
    setSaving(true);
    setRoleMenuError(null);
    try {
      await onReplaceRoleMenus(roleMenuRecord.id, ids);
      setRoleMenuRecord(null);
      setOperationMessage({ type: 'success', text: '角色菜单保存成功' });
    } catch (err) {
      const message = readErrorMessage(err);
      setRoleMenuError(message);
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
            if (isOperationLogMenuRecord(parent)) {
              setOperationMessage({ type: 'error', text: '操作日志菜单仅允许查询，不允许修改' });
              return;
            }
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
                        {!isMenuManagement || !isOperationLogMenuRecord(record) ? (
                          <button type="button" onClick={() => openEdit(record)} disabled={saving}>
                            <Pencil size={14} />
                            修改
                          </button>
                        ) : null}
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
                        {menu?.path === '/api/manager/system/roles' ? (
                          <button type="button" onClick={() => openRoleMenuDialog(record)} disabled={saving}>
                            <ListTree size={14} />
                            分配菜单
                          </button>
                        ) : null}
                        {menu?.path === '/api/manager/system/menus' && !isOperationLogMenuRecord(record) ? (
                          <button type="button" onClick={() => openPermissionDialog(record)} disabled={saving}>
                            <KeyRound size={14} />
                            分配权限
                          </button>
                        ) : null}
                        {!isMenuManagement || !isOperationLogMenuRecord(record) ? (
                          <button className="danger" type="button" onClick={() => deleteRecord(record)} disabled={saving}>
                            <Trash2 size={14} />
                            删除
                          </button>
                        ) : (
                          <span className="muted-pill">只读</span>
                        )}
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
          menuTree={permissionMenuTree}
          selectedIds={selectedPermissionIds}
          saving={saving}
          error={permissionError}
          onClose={() => {
            setPermissionRecord(null);
            setPermissionError(null);
            setPermissionMenuTree([]);
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
      {roleMenuRecord ? (
        <RoleMenuDialog
          record={roleMenuRecord}
          menus={roleMenuOptions}
          selectedIds={selectedRoleMenuIds}
          saving={saving}
          error={roleMenuError}
          onClose={() => {
            setRoleMenuRecord(null);
            setRoleMenuError(null);
          }}
          onSave={saveRoleMenus}
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
  const selectedMenuReadonly = selectedMenu ? isOperationLogMenuRecord(selectedMenu) : false;
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
                {selectedMenuReadonly ? (
                  <span className="muted-pill">只读</span>
                ) : (
                  <>
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
                  </>
                )}
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
  meta?: string;
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
  menuTree,
  selectedIds,
  saving,
  error,
  onClose,
  onSave,
}: {
  record: SystemPageRecord;
  permissions: SysPermission[];
  menuTree: MenuTreeNode[];
  selectedIds: Set<number>;
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onSave: (ids: number[]) => void;
}) {
  const [nextIds, setNextIds] = useState<Set<number>>(() => new Set(selectedIds));
  const permissionByCode = new Map(
    permissions
      .filter((permission) => Boolean(permission.permissionCode))
      .map((permission) => [permission.permissionCode as string, permission])
  );
  const matchedPermissionIds = new Set<number>();
  const visibleMenuTree = menuTree.filter((node) => menuNodeHasPermission(node, permissionByCode, matchedPermissionIds));
  const unmatchedPermissions = permissions.filter((permission) => !matchedPermissionIds.has(permission.id));

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
      <section className="record-dialog" role="dialog" aria-modal="true" aria-label="分配权限">
        <div className="dialog-heading">
          <div>
            <h3>分配权限</h3>
            <p>{recordLabel(record)}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={saving} title="关闭">
            <X size={18} />
          </button>
        </div>
        <form className="record-form single-column" onSubmit={submit}>
          <div className="permission-menu-tree">
            {visibleMenuTree.length > 0 ? (
              visibleMenuTree.map((node) => (
                <MenuPermissionTreeNode
                  key={node.id}
                  node={node}
                  level={0}
                  permissionByCode={permissionByCode}
                  selectedIds={nextIds}
                  saving={saving}
                  onToggle={toggle}
                />
              ))
            ) : (
              <div className="empty-state compact">暂无可选权限。</div>
            )}
            {unmatchedPermissions.length > 0 ? (
              <div className="permission-menu-unmatched">
                <div className="permission-menu-group" style={{ paddingLeft: 12 }}>
                  <FolderTree size={15} />
                  <span>
                    <strong>未关联菜单</strong>
                    <small>{unmatchedPermissions.length} 个权限</small>
                  </span>
                </div>
                {unmatchedPermissions.map((permission) => (
                  <PermissionCheckbox
                    key={permission.id}
                    permission={permission}
                    level={1}
                    checked={nextIds.has(permission.id)}
                    saving={saving}
                    onToggle={toggle}
                  />
                ))}
              </div>
            ) : null}
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

function MenuPermissionTreeNode({
  node,
  level,
  permissionByCode,
  selectedIds,
  saving,
  onToggle,
}: {
  node: MenuTreeNode;
  level: number;
  permissionByCode: Map<string, SysPermission>;
  selectedIds: Set<number>;
  saving: boolean;
  onToggle: (id: number) => void;
}) {
  const permission = node.path ? permissionByCode.get(node.path) : undefined;
  const children = node.children.filter((child) => menuNodeHasPermission(child, permissionByCode));

  return (
    <div className="permission-menu-tree-item">
      {permission ? (
        <PermissionCheckbox
          permission={permission}
          level={level}
          checked={selectedIds.has(permission.id)}
          saving={saving}
          onToggle={onToggle}
          menuName={node.menuName}
        />
      ) : (
        <div className="permission-menu-group" style={{ paddingLeft: 12 + level * 22 }}>
          <FolderTree size={15} />
          <span>
            <strong>{node.menuName || node.menuCode || `菜单 ${node.id}`}</strong>
            <small>{node.menuCode || node.path || node.id}</small>
          </span>
        </div>
      )}
      {children.length > 0 ? (
        <div className="permission-menu-tree-children">
          {children.map((child) => (
            <MenuPermissionTreeNode
              key={child.id}
              node={child}
              level={level + 1}
              permissionByCode={permissionByCode}
              selectedIds={selectedIds}
              saving={saving}
              onToggle={onToggle}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function PermissionCheckbox({
  permission,
  level,
  checked,
  saving,
  onToggle,
  menuName,
}: {
  permission: SysPermission;
  level: number;
  checked: boolean;
  saving: boolean;
  onToggle: (id: number) => void;
  menuName?: string;
}) {
  return (
    <label className="permission-menu-option" style={{ paddingLeft: 12 + level * 22 }}>
      <input
        type="checkbox"
        checked={checked}
        onChange={() => onToggle(permission.id)}
        disabled={saving}
      />
      <span className="permission-menu-option-main">
        <strong>{menuName || permission.permissionName || permission.permissionCode}</strong>
        <small>{permission.permissionName ? `${permission.permissionName} · ${permission.permissionCode}` : permission.permissionCode}</small>
      </span>
      <span className="status-pill">权限</span>
    </label>
  );
}

function menuNodeHasPermission(
  node: MenuTreeNode,
  permissionByCode: Map<string, SysPermission>,
  matchedPermissionIds?: Set<number>
): boolean {
  const permission = node.path ? permissionByCode.get(node.path) : undefined;
  if (permission) {
    matchedPermissionIds?.add(permission.id);
    return true;
  }
  return node.children.some((child) => menuNodeHasPermission(child, permissionByCode, matchedPermissionIds));
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

function RoleMenuDialog({
  record,
  menus,
  selectedIds,
  saving,
  error,
  onClose,
  onSave,
}: {
  record: SystemPageRecord;
  menus: MenuTreeNode[];
  selectedIds: Set<number>;
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onSave: (ids: number[]) => void;
}) {
  const [nextIds, setNextIds] = useState<Set<number>>(() => new Set(selectedIds));

  function toggle(menuId: number) {
    setNextIds((current) => {
      const next = new Set(current);
      if (next.has(menuId)) {
        next.delete(menuId);
      } else {
        next.add(menuId);
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
      <section className="record-dialog" role="dialog" aria-modal="true" aria-label="分配菜单">
        <div className="dialog-heading">
          <div>
            <h3>分配菜单</h3>
            <p>{recordLabel(record)}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={saving} title="关闭">
            <X size={18} />
          </button>
        </div>
        <form className="record-form single-column" onSubmit={submit}>
          <div className="role-menu-tree">
            {menus.length > 0 ? (
              menus.map((menuItem) => (
                <RoleMenuTreeOption
                  key={menuItem.id}
                  node={menuItem}
                  level={0}
                  selectedIds={nextIds}
                  saving={saving}
                  onToggle={toggle}
                />
              ))
            ) : (
              <div className="empty-state compact">暂无可选菜单。</div>
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

function RoleMenuTreeOption({
  node,
  level,
  selectedIds,
  saving,
  onToggle,
}: {
  node: MenuTreeNode;
  level: number;
  selectedIds: Set<number>;
  saving: boolean;
  onToggle: (id: number) => void;
}) {
  return (
    <div className="role-menu-tree-item">
      <label className="role-menu-option" style={{ paddingLeft: 12 + level * 22 }}>
        <input
          type="checkbox"
          checked={selectedIds.has(node.id)}
          onChange={() => onToggle(node.id)}
          disabled={saving}
        />
        <span className="role-menu-option-main">
          <strong>{node.menuName || node.menuCode || `菜单 ${node.id}`}</strong>
          <small>{node.buttonFlag ? '按钮' : '菜单'} · {node.menuCode || node.id}</small>
        </span>
        <span className={node.buttonFlag ? 'muted-pill' : 'status-pill'}>{node.buttonFlag ? '按钮' : '菜单'}</span>
      </label>
      {node.children.length > 0 ? (
        <div className="role-menu-tree-children">
          {node.children.map((child) => (
            <RoleMenuTreeOption
              key={child.id}
              node={child}
              level={level + 1}
              selectedIds={selectedIds}
              saving={saving}
              onToggle={onToggle}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function OperationLogDetailDialog({ record, onClose }: { record: OperationLog; onClose: () => void }) {
  const before = parseLogData(readLogPayload(record, 'beforeData', 'before_data'));
  const after = parseLogData(readLogPayload(record, 'afterData', 'after_data'));
  const operationType = readLogText(record, 'operationType', 'operation_type');
  const businessType = readLogText(record, 'businessType', 'business_type');
  const rows = buildLogCompareRows(before, after, businessType);
  const changedRows = rows.filter((row) => row.changeType !== 'same');

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="record-dialog log-detail-dialog" role="dialog" aria-modal="true" aria-label="操作日志详情">
        <div className="dialog-heading">
          <div>
            <h3>操作日志详情</h3>
            <p>ID {record.id} · {formatDate(readLogText(record, 'operationAt', 'operation_at'))}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} title="关闭">
            <X size={18} />
          </button>
        </div>
        <div className="log-detail-body">
          <div className="log-meta-grid">
            <DetailItem label="操作类型" value={operationTypeLabel(operationType)} />
            <DetailItem label="操作者" value={readLogText(record, 'operatorUsername', 'operator_username')} />
            <DetailItem label="用户类型" value={readLogText(record, 'operatorUserType', 'operator_user_type')} />
            <DetailItem label="业务模块" value={readLogText(record, 'businessModule', 'business_module')} />
            <DetailItem label="业务类型" value={businessType} />
            <DetailItem label="业务 ID" value={readLogText(record, 'businessId', 'business_id')} />
            <DetailItem label="业务名称" value={readLogText(record, 'businessName', 'business_name')} />
            <DetailItem label="请求方法" value={readLogText(record, 'requestMethod', 'request_method')} />
            <DetailItem label="请求 URI" value={readLogText(record, 'requestUri', 'request_uri')} />
            <DetailItem label="客户端 IP" value={readLogText(record, 'clientIp', 'client_ip')} />
          </div>

          <div className="diff-summary">
            <span>{`页面字段 ${rows.length} 个，变更 ${changedRows.length} 个`}</span>
            <small>新增 {rows.filter((row) => row.changeType === 'added').length} · 删除 {rows.filter((row) => row.changeType === 'removed').length} · 修改 {rows.filter((row) => row.changeType === 'modified').length}</small>
          </div>

          <div className="diff-card-list">
            {rows.length > 0 ? rows.map((row) => (
              <DiffCard key={row.key} row={row} />
            )) : (
              <div className="empty-state compact">暂无页面字段。</div>
            )}
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

function DiffCard({ row }: { row: CompareRow }) {
  return (
    <div className={`diff-card ${row.changeType}`}>
      <div className="diff-card-head">
        <strong>{row.label}</strong>
        <span>{changeTypeLabel(row.changeType)}</span>
      </div>
      <div className="diff-highlight">
        {renderDiffHighlight(row)}
      </div>
      <div className="diff-values">
        <div className="diff-value before">
          <span>操作前</span>
          <code>{row.beforeValue}</code>
        </div>
        <div className="diff-arrow">→</div>
        <div className="diff-value after">
          <span>操作后</span>
          <code>{row.afterValue}</code>
        </div>
      </div>
    </div>
  );
}

function renderDiffHighlight(row: CompareRow) {
  if (row.changeType === 'added') {
    return (
      <>
        <span className="diff-action-label">新增</span>
        <code>{row.afterValue}</code>
      </>
    );
  }
  if (row.changeType === 'removed') {
    return (
      <>
        <span className="diff-action-label">删除</span>
        <code>{row.beforeValue}</code>
      </>
    );
  }
  if (row.changeType === 'same') {
    return (
      <>
        <span className="diff-action-label">未变化</span>
        <code>{row.afterValue}</code>
      </>
    );
  }
  return (
    <>
      <code className="diff-from">{row.beforeValue}</code>
      <span className="diff-action-label">改为</span>
      <code className="diff-to">{row.afterValue}</code>
    </>
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

function isOperationLogMenuRecord(record: SystemPageRecord) {
  return readField(record, 'menuCode') === 'system-operation-logs'
    || readField(record, 'path') === '/api/manager/system/operation-logs';
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
  value: unknown;
  flattened: Record<string, string>;
  raw: string;
};

type CompareRow = {
  key: string;
  label: string;
  beforeValue: string;
  afterValue: string;
  changeType: 'added' | 'removed' | 'modified' | 'same';
};

function readLogPayload(record: OperationLog, camelKey: string, snakeKey: string) {
  const values = record as Record<string, unknown>;
  return values[camelKey] ?? values[snakeKey];
}

function readLogText(record: OperationLog, camelKey: string, snakeKey: string) {
  const value = readLogPayload(record, camelKey, snakeKey);
  return value === null || value === undefined || value === '' ? undefined : String(value);
}

function parseLogData(value?: unknown): ParsedLogData {
  if (value === null || value === undefined || value === '') {
    return { value: undefined, flattened: {}, raw: '' };
  }
  if (typeof value !== 'string') {
    return {
      value,
      flattened: flattenLogValue(value),
      raw: formatLogValue(value),
    };
  }
  if (!value.trim()) {
    return { value: undefined, flattened: {}, raw: '' };
  }
  try {
    const parsed = parsePossiblyNestedJson(value);
    return {
      value: parsed,
      flattened: flattenLogValue(parsed),
      raw: formatLogValue(parsed),
    };
  } catch {
    return {
      value,
      flattened: { 原始数据: value },
      raw: value,
    };
  }
}

function parsePossiblyNestedJson(value: string): unknown {
  let parsed: unknown = JSON.parse(value);
  for (let index = 0; index < 2; index += 1) {
    if (typeof parsed !== 'string') {
      break;
    }
    const trimmed = parsed.trim();
    if (!trimmed || (!trimmed.startsWith('{') && !trimmed.startsWith('['))) {
      break;
    }
    parsed = JSON.parse(trimmed) as unknown;
  }
  return parsed;
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

function buildLogCompareRows(before: ParsedLogData, after: ParsedLogData, businessType?: string): CompareRow[] {
  const relationRows = buildRelationCompareRows(before.value, after.value, businessType);
  if (relationRows) {
    return relationRows;
  }
  const labels = operationLogFieldLabels(businessType);
  const fieldOrder = Object.keys(labels);
  const knownPageFields = new Set(fieldOrder);
  const supportsPageFields = fieldOrder.length > 0;
  const keys = (supportsPageFields
    ? fieldOrder
    : Array.from(new Set([...Object.keys(before.flattened), ...Object.keys(after.flattened)])))
    .filter((key) => !supportsPageFields || knownPageFields.has(baseLogFieldKey(key)))
    .sort((left, right) => {
      const leftIndex = fieldOrder.indexOf(baseLogFieldKey(left));
      const rightIndex = fieldOrder.indexOf(baseLogFieldKey(right));
      if (leftIndex !== rightIndex) {
        return leftIndex - rightIndex;
      }
      return left.localeCompare(right);
    });
  if (keys.length === 0 && (before.raw || after.raw)) {
    keys.push('原始数据');
  }
  const rows = keys.map((key) => {
    const beforeValue = displayLogFieldValue(key, readFlattenedLogField(before.flattened, key) ?? (key === '原始数据' ? before.raw : '-'));
    const afterValue = displayLogFieldValue(key, readFlattenedLogField(after.flattened, key) ?? (key === '原始数据' ? after.raw : '-'));
    return {
      key,
      label: operationLogFieldLabel(labels, businessType, key),
      beforeValue,
      afterValue,
      changeType: logChangeType(beforeValue, afterValue),
    };
  });
  if (rows.every((row) => row.changeType === 'same') && before.raw !== after.raw && (before.raw || after.raw)) {
    rows.push({
      key: 'raw-diff',
      label: '修改内容',
      beforeValue: before.raw || '-',
      afterValue: after.raw || '-',
      changeType: logChangeType(before.raw || '-', after.raw || '-'),
    });
  }
  return rows;
}

function readFlattenedLogField(flattened: Record<string, string>, key: string) {
  const variants = logFieldKeyVariants(key);
  for (const variant of variants) {
    if (flattened[variant] !== undefined) {
      return flattened[variant];
    }
  }
  const matchedKey = Object.keys(flattened).find((item) => variants.includes(baseLogFieldKey(item)));
  return matchedKey ? flattened[matchedKey] : undefined;
}

function logFieldKeyVariants(key: string) {
  const baseKey = baseLogFieldKey(key);
  return Array.from(new Set([
    key,
    baseKey,
    camelToSnakeCase(key),
    camelToSnakeCase(baseKey),
    snakeToCamelCase(key),
    snakeToCamelCase(baseKey),
  ]));
}

function buildRelationCompareRows(beforeValue: unknown, afterValue: unknown, businessType?: string): CompareRow[] | null {
  const config = relationCompareConfig(businessType);
  if (!config) {
    return null;
  }
  const beforeItems = Array.isArray(beforeValue) ? beforeValue : [];
  const afterItems = Array.isArray(afterValue) ? afterValue : [];
  const beforeMap = new Map(beforeItems.map((item, index) => [relationItemKey(item, index), item]));
  const afterMap = new Map(afterItems.map((item, index) => [relationItemKey(item, index), item]));
  const keys = Array.from(new Set([...beforeMap.keys(), ...afterMap.keys()])).sort((left, right) => {
    const leftItem = afterMap.get(left) ?? beforeMap.get(left);
    const rightItem = afterMap.get(right) ?? beforeMap.get(right);
    return relationItemTitle(leftItem, config).localeCompare(relationItemTitle(rightItem, config), 'zh-Hans-CN');
  });

  return keys.map((key) => {
    const beforeItem = beforeMap.get(key);
    const afterItem = afterMap.get(key);
    const item = afterItem ?? beforeItem;
    return {
      key,
      label: relationItemTitle(item, config),
      beforeValue: beforeItem ? relationItemSummary(beforeItem, config) : '-',
      afterValue: afterItem ? relationItemSummary(afterItem, config) : '-',
      changeType: logChangeType(beforeItem ? relationItemSummary(beforeItem, config) : '-', afterItem ? relationItemSummary(afterItem, config) : '-'),
    };
  });
}

function relationCompareConfig(businessType?: string) {
  const configs: Record<string, { title: string; nameKeys: string[]; codeKeys: string[]; extraKeys: string[] }> = {
    SYS_USER_ROLE: {
      title: '用户角色',
      nameKeys: ['roleName'],
      codeKeys: ['roleCode'],
      extraKeys: ['status'],
    },
    SYS_ROLE_PERMISSION: {
      title: '角色权限',
      nameKeys: ['permissionName'],
      codeKeys: ['permissionCode'],
      extraKeys: ['description', 'status'],
    },
    SYS_ROLE_MENU: {
      title: '角色菜单',
      nameKeys: ['menuName'],
      codeKeys: ['menuCode'],
      extraKeys: ['path', 'component', 'buttonFlag', 'visible', 'status'],
    },
    SYS_MENU_PERMISSION: {
      title: '菜单权限',
      nameKeys: ['permissionName'],
      codeKeys: ['permissionCode'],
      extraKeys: ['description', 'status'],
    },
  };
  return businessType ? configs[businessType] : undefined;
}

function relationItemKey(item: unknown, index: number) {
  if (item && typeof item === 'object') {
    const record = item as Record<string, unknown>;
    const stableValue = readObjectField(record, 'id')
      ?? readObjectField(record, 'permissionCode')
      ?? readObjectField(record, 'menuCode')
      ?? readObjectField(record, 'roleCode')
      ?? readObjectField(record, 'username');
    if (stableValue !== undefined && stableValue !== null && stableValue !== '') {
      return String(stableValue);
    }
  }
  return `index-${index}`;
}

function relationItemTitle(item: unknown, config: { title: string; nameKeys: string[]; codeKeys: string[] }) {
  if (!item || typeof item !== 'object') {
    return config.title;
  }
  const record = item as Record<string, unknown>;
  const name = firstTextValue(record, config.nameKeys);
  const code = firstTextValue(record, config.codeKeys);
  if (name && code) {
    return `${name}（${code}）`;
  }
  return name || code || `${config.title} ${textValueFromUnknown(readObjectField(record, 'id'))}`;
}

function relationItemSummary(item: unknown, config: { nameKeys: string[]; codeKeys: string[]; extraKeys: string[] }) {
  if (!item || typeof item !== 'object') {
    return formatLogValue(item);
  }
  const record = item as Record<string, unknown>;
  const lines = [
    ...config.nameKeys.map((key) => relationSummaryLine(record, key)),
    ...config.codeKeys.map((key) => relationSummaryLine(record, key)),
    ...config.extraKeys.map((key) => relationSummaryLine(record, key)),
  ].filter(Boolean);
  return lines.length > 0 ? lines.join('\n') : formatLogValue(item);
}

function relationSummaryLine(record: Record<string, unknown>, key: string) {
  const value = textValueFromUnknown(readObjectField(record, key));
  if (!value) {
    return '';
  }
  return `${logFieldDisplayLabel(key)}：${displayLogFieldValue(key, value)}`;
}

function firstTextValue(record: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = textValueFromUnknown(readObjectField(record, key));
    if (value) {
      return value;
    }
  }
  return '';
}

function readObjectField(record: Record<string, unknown>, key: string) {
  for (const variant of logFieldKeyVariants(key)) {
    if (record[variant] !== undefined) {
      return record[variant];
    }
  }
  return undefined;
}

function textValueFromUnknown(value: unknown) {
  return value === null || value === undefined || value === '' ? '' : String(value);
}

function operationLogFieldLabels(businessType: string | undefined) {
  const labelsByBusinessType: Record<string, Record<string, string>> = {
    SYS_USER: {
      username: '账号',
      password: '密码',
      displayName: '显示名称',
      email: '邮箱',
      status: '状态',
    },
    SYS_ROLE: {
      roleCode: '角色编码',
      roleName: '角色名称',
      description: '描述',
      status: '状态',
    },
    SYS_PERMISSION: {
      permissionCode: '权限编码',
      permissionName: '权限名称',
      description: '描述',
      status: '状态',
    },
    SYS_MENU: {
      parentId: '父菜单',
      menuCode: '菜单编码',
      menuName: '菜单名称',
      path: '路径',
      component: '组件',
      icon: '图标',
      menuLevel: '菜单级别',
      buttonFlag: '按钮标识',
      sortOrder: '排序',
      visible: '显示',
      status: '状态',
    },
    VIP_USER: {
      username: '账号',
      password: '密码',
      displayName: '显示名称',
      email: '邮箱',
      phone: '手机号',
      vipLevel: '会员等级',
      status: '状态',
    },
  };
  return businessType ? labelsByBusinessType[businessType] || {} : {};
}

function operationLogFieldLabel(labels: Record<string, string>, businessType: string | undefined, key: string) {
  const relationLabels: Record<string, string> = {
    SYS_USER_ROLE: '用户角色',
    SYS_ROLE_PERMISSION: '角色权限',
    SYS_ROLE_MENU: '角色菜单',
    SYS_MENU_PERMISSION: '菜单权限',
  };
  if (businessType && relationLabels[businessType]) {
    return key === '原始数据' ? relationLabels[businessType] : `${relationLabels[businessType]} ${key}`;
  }
  const baseKey = baseLogFieldKey(key);
  return labels[baseKey]
    || key;
}

function logFieldDisplayLabel(key: string) {
  const labels: Record<string, string> = {
    id: 'ID',
    username: '账号',
    roleCode: '角色编码',
    roleName: '角色名称',
    permissionCode: '权限编码',
    permissionName: '权限名称',
    menuCode: '菜单编码',
    menuName: '菜单名称',
    path: '路径',
    component: '组件',
    description: '描述',
    buttonFlag: '按钮标识',
    visible: '显示',
    status: '状态',
  };
  return labels[baseLogFieldKey(key)] || key;
}

function displayLogFieldValue(key: string, value: string) {
  const baseKey = baseLogFieldKey(key);
  if (value === '-') {
    return value;
  }
  if (baseKey === 'status') {
    return value === 'ENABLED' ? '启用' : value === 'DISABLED' ? '禁用' : value;
  }
  if (baseKey === 'buttonFlag') {
    return value === 'true' ? '按钮' : value === 'false' ? '菜单' : value;
  }
  if (baseKey === 'visible') {
    return value === 'true' ? '显示' : value === 'false' ? '隐藏' : value;
  }
  return value;
}

function baseLogFieldKey(key: string) {
  const withoutArrayPrefix = key.replace(/^\[\d+]\./, '');
  const segments = withoutArrayPrefix.split('.');
  return segments[segments.length - 1] || key;
}

function camelToSnakeCase(value: string) {
  return value.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
}

function snakeToCamelCase(value: string) {
  return value.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase());
}

function logChangeType(beforeValue: string, afterValue: string): CompareRow['changeType'] {
  if (beforeValue === afterValue) {
    return 'same';
  }
  if (beforeValue === '-') {
    return 'added';
  }
  if (afterValue === '-') {
    return 'removed';
  }
  return 'modified';
}

function changeTypeLabel(type: CompareRow['changeType']) {
  const labels: Record<CompareRow['changeType'], string> = {
    added: '新增',
    removed: '删除',
    modified: '修改',
    same: '未变化',
  };
  return labels[type];
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
