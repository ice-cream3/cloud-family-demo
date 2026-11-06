import { useId, useState } from 'react';
import { ChevronDown, Search, X } from 'lucide-react';

export type MerchantTypeOption = {
  label: string; value: string; name?: string; disabled?: boolean;
  parentValue?: string | null;
  categoryCode?: string; categoryName?: string; categorySortOrder?: number;
};

export function MerchantTypeMultiSelect({ options, values, onChange, disabled, loading }: {
  options: MerchantTypeOption[]; values: string[]; onChange: (codes: string[]) => void;
  disabled: boolean; loading: boolean;
}) {
  const [expanded, setExpanded] = useState(false);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const panelId = useId();
  const keyword = query.trim().toLowerCase();
  const selected = new Set(values);
  const title = (option: MerchantTypeOption) => option.name || option.label.replace(/（[^）]*）/, '');
  const groups = new Map<string, { code: string; name: string; order: number; options: MerchantTypeOption[] }>();
  for (const option of options) {
    const code = option.categoryCode || (option.disabled ? 'UNAVAILABLE' : 'OTHER');
    if (!groups.has(code)) groups.set(code, {code, name: option.categoryName || (option.disabled ? '已停用类型' : '其他类型'), order: option.categorySortOrder ?? 999, options: []});
    groups.get(code)!.options.push(option);
  }
  const categories = [...groups.values()].sort((a, b) => a.order - b.order);
  const visible = categories.filter((group) => keyword || !category || group.code === category)
    .map((group) => ({...group, options: group.options.filter((option) => `${title(option)} ${option.value} ${group.name}`.toLowerCase().includes(keyword))}))
    .filter((group) => group.options.length);
  const invalid = values.some((value) => options.find((option) => option.value === value)?.disabled);
  function toggle(code: string) {
    if (disabled) return;
    if (selected.has(code)) onChange(values.filter((value) => value !== code));
    else if (values.length < 20 && !options.find((option) => option.value === code)?.disabled) onChange([...values, code]);
  }
  return <div className="manager-type-selector" aria-busy={loading}>
    <div className="manager-type-summary"><span>经营范围 <strong>{values.length} / 20</strong></span>
      <button type="button" className="manager-type-expand" disabled={disabled} aria-expanded={expanded} aria-controls={panelId} onClick={() => setExpanded(!expanded)}>{loading ? '加载中…' : expanded ? '收起选择' : '选择类型'}<ChevronDown size={14} className={expanded ? 'expanded' : ''} /></button>
    </div>
    {values.length ? <div className="manager-type-selected" role="group" aria-label="已选商家类型">
      {(expanded ? values : values.slice(0, 6)).map((value) => {
        const option = options.find((item) => item.value === value);
        return <button type="button" disabled={disabled} key={value} className={option?.disabled ? 'is-unavailable' : ''} onClick={() => toggle(value)} aria-label={`移除商家类型：${option ? title(option) : value}`} title={value}>{option ? title(option) : value}{option?.disabled ? <small>已停用</small> : null}<X size={12} aria-hidden="true" /></button>;
      })}
      {!expanded && values.length > 6 ? <button type="button" disabled={disabled} className="manager-type-more" onClick={() => setExpanded(true)}>还有 {values.length - 6} 项</button> : null}
    </div> : <p className="manager-type-placeholder">请选择至少一个已启用的商家类型</p>}
    <div id={panelId} hidden={!expanded}>
      {expanded ? <>
        <div className="manager-type-search"><Search size={15} aria-hidden="true" /><input type="search" aria-label="搜索商家类型" placeholder="搜索名称、编码或经营大类" value={query} disabled={disabled} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') event.preventDefault(); }} /></div>
        <div className="manager-type-browser">
          <nav aria-label="商家经营大类"><button type="button" disabled={disabled} aria-pressed={!category || Boolean(keyword)} className={!category || keyword ? 'active' : ''} onClick={() => { setCategory(''); setQuery(''); }}>全部类型</button>
            {categories.map((group) => <button type="button" disabled={disabled} key={group.code} aria-pressed={category === group.code && !keyword} className={category === group.code && !keyword ? 'active' : ''} onClick={() => { setCategory(group.code); setQuery(''); }}>{group.name}<small>{group.options.filter((option) => selected.has(option.value)).length || ''}</small></button>)}
          </nav>
          <div className="manager-type-groups">
            {visible.map((group) => <section key={group.code} aria-label={group.name}><h3>{group.name}<small>{group.options.length} 类</small></h3><div className="manager-type-options">
              {group.options.map((option) => <label key={option.value} className={`manager-type-choice ${selected.has(option.value) ? 'is-selected' : ''} ${option.disabled ? 'is-unavailable' : ''}`}>
                <input type="checkbox" checked={selected.has(option.value)} disabled={disabled || (!selected.has(option.value) && (Boolean(option.disabled) || values.length >= 20))} onChange={() => toggle(option.value)} />
                <span><strong>{title(option)}</strong><small title={option.value}>{option.value}</small></span>
              </label>)}
            </div></section>)}
            {!visible.length ? <p className="manager-type-placeholder" role="status">{loading ? '类型加载中…' : keyword ? '暂无匹配类型' : '暂无可用类型，请先在字典管理中添加'}</p> : null}
          </div>
        </div>
        <div className="manager-type-selection-actions"><button type="button" disabled={disabled || !values.length} onClick={() => onChange([])}>清空选择</button><span>{values.length >= 20 ? '已达20项上限，请先取消再选择' : '支持跨经营大类多选'}</span></div>
      </> : null}
    </div>
    {invalid ? <p className="manager-type-invalid" role="alert">包含已停用类型，请先移除后保存。</p> : null}
    <p className="manager-type-warning">移除类型会自动下架该商家的相关上架商品。</p>
  </div>;
}
