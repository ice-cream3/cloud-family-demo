import { useEffect, useId, useRef, useState } from 'react';
import { Check, ChevronDown, ChevronRight, FolderTree, Search } from 'lucide-react';

type Option = { label: string; value: string; parentValue?: string | null; disabled?: boolean };
type TreeNode = { option: Option; children: TreeNode[] };

function buildTree(options: Option[]): TreeNode[] {
  const nodes = new Map(options.map((option) => [option.value, { option, children: [] } as TreeNode]));
  const roots: TreeNode[] = [];
  for (const node of nodes.values()) {
    const parent = node.option.parentValue ? nodes.get(node.option.parentValue) : undefined;
    // Malformed or cyclic ancestry must not hide an option or recurse forever.
    const seen = new Set([node.option.value]);
    let ancestor = parent;
    while (ancestor && !seen.has(ancestor.option.value)) {
      seen.add(ancestor.option.value);
      ancestor = ancestor.option.parentValue ? nodes.get(ancestor.option.parentValue) : undefined;
    }
    if (parent && !ancestor) parent.children.push(node);
    else roots.push(node);
  }
  return roots;
}

export function ParentCategorySelect({ value, options, disabled, onChange, mode = 'parent', placeholder }: {
  mode?: 'parent' | 'dictionary';
  placeholder?: string;
  value: string;
  options: Option[];
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const treeId = useId();
  useEffect(() => {
    if (!open) return;
    const hideOutside = (event: Event) => {
      if (event.target instanceof Node && !root.current?.contains(event.target)) setOpen(false);
    };
    const hide = () => setOpen(false);
    // Blank areas do not always receive focus, so blur alone cannot dismiss the menu.
    document.addEventListener('pointerdown', hideOutside, true);
    document.addEventListener('focusin', hideOutside, true);
    window.addEventListener('blur', hide);
    return () => {
      document.removeEventListener('pointerdown', hideOutside, true);
      document.removeEventListener('focusin', hideOutside, true);
      window.removeEventListener('blur', hide);
    };
  }, [open]);
  const selected = options.find((option) => option.value === value);
  const fieldLabel = mode === 'parent' ? '上级大类' : '字典类型';
  const label = selected?.label || value || placeholder || '无（大类或独立字典）';
  const title = (option: Option) => option.value ? option.label.replace(/（[^）]*）/, '') : option.label;
  const keyword = query.trim().toLocaleLowerCase();
  function filterTree(nodes: TreeNode[]): TreeNode[] {
    return nodes.flatMap((node) => {
      if (`${node.option.label} ${node.option.value}`.toLocaleLowerCase().includes(keyword)) return [node];
      const children = filterTree(node.children);
      return children.length ? [{...node, children}] : [];
    });
  }
  const fullTree = buildTree(options);
  const tree = keyword ? filterTree(fullTree) : fullTree;
  function unavailable(option: Option) {
    return Boolean(option.disabled || (mode === 'parent' ? option.parentValue : option.value === 'ECOMMERCE' || options.some((item) => item.parentValue === option.value)));
  }
  const items = () => Array.from(root.current?.querySelectorAll<HTMLDivElement>('[role="treeitem"]') || []);
  const focusValue = (code: string) => items().find((item) => item.dataset.value === code)?.focus();
  function toggle(code: string, expand?: boolean) {
    setCollapsed((current) => {
      const next = new Set(current);
      if (expand ?? next.has(code)) next.delete(code); else next.add(code);
      return next;
    });
  }
  function select(option: Option) {
    if (unavailable(option)) return;
    onChange(option.value); setOpen(false); trigger.current?.focus();
  }
  function renderNode(node: TreeNode, level: number): React.ReactNode {
    const { option, children } = node;
    const expanded = Boolean(keyword) || !collapsed.has(option.value);
    const isUnavailable = unavailable(option);
    return <div key={option.value} role="treeitem" data-value={option.value} tabIndex={option.value === value ? 0 : -1}
      aria-level={level} aria-selected={option.value === value} aria-disabled={isUnavailable}
      aria-expanded={children.length ? expanded : undefined}
      onClick={(event) => { event.preventDefault(); event.stopPropagation(); select(option); }}
      onKeyDown={(event) => {
        if (event.target !== event.currentTarget) return;
        if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); event.stopPropagation(); select(option); }
        if (event.key === 'ArrowRight') {
          event.preventDefault(); event.stopPropagation();
          if (children.length && !expanded) toggle(option.value, true);
          else if (children.length) focusValue(children[0].option.value);
        }
        if (event.key === 'ArrowLeft') {
          event.preventDefault(); event.stopPropagation();
          if (children.length && expanded) toggle(option.value, false);
          else if (option.parentValue) focusValue(option.parentValue);
        }
      }}>
      <div className="parent-category-option" style={{ paddingLeft: 8 + (level - 1) * 20 }}>
        {children.length ? <button type="button" className="parent-category-expander" tabIndex={-1}
          aria-label={`${expanded ? '收起' : '展开'}${title(option)}`}
          onClick={(event) => {
            event.preventDefault(); event.stopPropagation(); focusValue(option.value); toggle(option.value);
          }}><ChevronRight size={14} className={expanded ? 'expanded' : ''} /></button>
          : <span className="parent-category-leaf" aria-hidden="true" />}
        <FolderTree size={15} className="parent-category-node-icon" aria-hidden="true" />
        <span className="parent-category-option-text"><span>{title(option)}</span>
          <small>{option.value || '作为大类或独立字典创建'}</small></span>
        {option.value === value ? <Check size={16} aria-hidden="true" /> : null}
      </div>
      {children.length && expanded ? <div role="group">{children.map((child) => renderNode(child, level + 1))}</div> : null}
    </div>;
  }

  return <div className="parent-category-select" ref={root}
    onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false); }}
    onKeyDown={(event) => {
      if (event.key === 'Escape') { event.preventDefault(); setOpen(false); trigger.current?.focus(); }
      if ((event.target as HTMLElement).tagName === 'INPUT') return;
      if (!open || !['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
      event.preventDefault();
      const nodes = items();
      const current = nodes.indexOf(document.activeElement as HTMLDivElement);
      const index = event.key === 'Home' ? 0 : event.key === 'End' ? nodes.length - 1 : event.key === 'ArrowUp' ? (current < 0 ? nodes.length - 1 : Math.max(0, current - 1)) : Math.min(nodes.length - 1, current + 1);
      nodes[index]?.focus();
    }}>
    <button className="parent-category-trigger" type="button" ref={trigger} disabled={disabled}
      aria-label={`${fieldLabel}：${label}`} aria-haspopup="tree" aria-expanded={open} aria-controls={open ? treeId : undefined}
      onClick={() => { setOpen(!open); setQuery(''); }}
      onKeyDown={(event) => {
        if (!open && ['ArrowDown', 'ArrowUp'].includes(event.key)) {
          event.preventDefault(); event.stopPropagation(); setOpen(true); setQuery('');
          requestAnimationFrame(() => { const active = items().find((item) => item.dataset.value === value); (active || items()[0])?.focus(); });
        }
      }}>
      <FolderTree size={17} aria-hidden="true" />
      <span className="parent-category-value">{selected ? title(selected) : label}</span>
      {value ? <span className="parent-category-code">{value}</span> : null}
      <ChevronDown size={16} className={open ? 'expanded' : ''} aria-hidden="true" />
    </button>
    {open && !disabled ? <div className="parent-category-menu">
      <div className="parent-category-caption">{mode === 'parent' ? '选择上级大类 · 子类型仅供查看' : '选择字典类型 · 按大类分组'}</div>
      {mode === 'dictionary' ? <div className="category-search-wrap">
        <Search size={15} aria-hidden="true" />
        <input className="category-search-input" type="search" aria-label="搜索字典类型" placeholder="搜索名称或编码"
          value={query} onChange={(event) => setQuery(event.target.value)}
          onKeyDown={(event) => { if (event.key === 'ArrowDown') { event.preventDefault(); items()[0]?.focus(); } }} />
      </div> : null}
      <div className="category-tree-scroll" id={treeId} role="tree" aria-label={fieldLabel}>{tree.map((node) => renderNode(node, 1))}</div>
      {!tree.length ? <div className="category-search-empty" role="status">暂无匹配类型</div> : null}
    </div> : null}
    <small className="parent-category-hint">{disabled ? `${fieldLabel}创建后不可修改` : mode === 'parent' ? '不选择上级时，作为大类或独立字典' : '选择具体分类后添加字典项'}</small>
  </div>;
}
