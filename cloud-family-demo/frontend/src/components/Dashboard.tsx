import { useEffect, useState } from 'react';
import { ChevronRight, FolderTree, LogOut, RefreshCw } from 'lucide-react';
import type { ManagerDashboard, MenuTreeNode, PageResult, SysUser, UserProfile } from '../types/api';
import type { Session } from '../services/tokenStore';

type DashboardProps = {
  session: Session;
  managerDashboard: ManagerDashboard | null;
  userProfile: UserProfile | null;
  menuTree: MenuTreeNode[];
  menuError: string | null;
  activeMenu: MenuTreeNode | null;
  userPage: PageResult<SysUser> | null;
  userPageLoading: boolean;
  userPageError: string | null;
  onMenuSelect: (menu: MenuTreeNode) => void;
  onUserPageChange: (pageNum: number) => void;
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
  userPage,
  userPageLoading,
  userPageError,
  onMenuSelect,
  onUserPageChange,
  loading,
  error,
  onReload,
  onLogout,
}: DashboardProps) {
  const menuCount = countMenuNodes(menuTree);

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
            <h1>服务工作台</h1>
            <p>登录身份、Gateway 转发和后端响应状态。</p>
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

        {activeMenu?.path === '/api/manager/system/users' ? (
          <UserPagePanel
            page={userPage}
            loading={userPageLoading}
            error={userPageError}
            onPageChange={onUserPageChange}
          />
        ) : (
          <MenuTreePanel nodes={menuTree} total={menuCount} error={menuError} />
        )}

        <div className="content-grid">
          <DataPanel title="用户服务 /api/users/me" data={userProfile} emptyText="当前账号暂无用户端响应" />
          <DataPanel title="管理服务 /api/manager/dashboard" data={managerDashboard} emptyText="非管理账号可能无法访问该接口" />
        </div>
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

function UserPagePanel({
  page,
  loading,
  error,
  onPageChange,
}: {
  page: PageResult<SysUser> | null;
  loading: boolean;
  error: string | null;
  onPageChange: (pageNum: number) => void;
}) {
  const records = page?.records || [];
  const pageNum = page?.pageNum || 1;
  const pageSize = page?.pageSize || 10;
  const total = page?.total || 0;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <article className="table-panel">
      <div className="section-heading">
        <div>
          <h2>用户管理</h2>
          <p>调用 /api/manager/system/users/page 展示系统用户分页数据。</p>
        </div>
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
      {error ? (
        <div className="empty-state compact">{error}</div>
      ) : records.length > 0 ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>账号</th>
                <th>显示名称</th>
                <th>邮箱</th>
                <th>状态</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              {records.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.username}</td>
                  <td>{user.displayName || '-'}</td>
                  <td>{user.email || '-'}</td>
                  <td>
                    <span className={user.status === 'ENABLED' ? 'status-pill' : 'muted-pill'}>{user.status || '-'}</span>
                  </td>
                  <td>{formatDate(user.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="empty-state compact">{loading ? '用户数据加载中。' : '暂无用户数据。'}</div>
      )}
    </article>
  );
}

function MenuTreePanel({ nodes, total, error }: { nodes: MenuTreeNode[]; total: number; error: string | null }) {
  return (
    <article className="menu-tree-panel">
      <div className="section-heading">
        <div>
          <h2>当前用户菜单树</h2>
          <p>调用 /api/manager/system/menus/current-user/tree，按登录用户角色授权展示。</p>
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
  return nodes.reduce((total, node) => total + 1 + countMenuNodes(node.children || []), 0);
}

function formatDate(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}
