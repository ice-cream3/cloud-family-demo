import { useRef, useState } from 'react';
import { Check, Search, SlidersHorizontal, X } from 'lucide-react';
import type { MerchantType } from '../services/commerceService';

type Category = { code: string; name: string; order: number; types: MerchantType[] };

export function MerchantTypeFilter({ types, values, onChange }: {
  types: MerchantType[]; values: string[]; onChange: (codes: string[]) => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const content = useRef<HTMLDivElement>(null);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const keyword = query.trim().toLowerCase();
  const groupMap = new Map<string, Category>();
  for (const type of types) {
    const code = type.categoryCode || 'OTHER';
    if (!groupMap.has(code)) groupMap.set(code, { code, name: type.categoryName || '其他类型', order: type.categorySortOrder ?? 999, types: [] });
    groupMap.get(code)!.types.push(type);
  }
  const groups = Array.from(groupMap.values()).sort((a, b) => a.order - b.order);
  const visibleGroups = groups.filter((group) => keyword || !category || group.code === category)
    .map((group) => ({ ...group, types: group.types.filter((type) => `${type.name} ${type.typeCode} ${group.name}`.toLowerCase().includes(keyword)) }))
    .filter((group) => group.types.length);
  function choose(code: string) { onChange(values.includes(code) ? values.filter((value) => value !== code) : [...values, code]); }
  function changeCategory(code: string) { setCategory(code); setQuery(''); content.current?.scrollTo({ top: 0 }); }
  return <div className="commerce-type-toolbar">
    <div className="showcase-categories" role="group" aria-label="商家类型筛选">
      <button type="button" className={!values.length ? 'active' : ''} aria-pressed={!values.length} onClick={() => onChange([])}>全部</button>
      {types.map((type) => <button type="button" key={type.typeCode} className={values.includes(type.typeCode) ? 'active' : ''} aria-pressed={values.includes(type.typeCode)} onClick={() => choose(type.typeCode)}>{type.name}</button>)}
    </div>
    <button className="commerce-filter-trigger" type="button" aria-haspopup="dialog" onClick={() => { setQuery(''); setCategory(''); dialog.current?.showModal(); content.current?.scrollTo({ top: 0 }); }}><SlidersHorizontal size={15} />分类{values.length ? ` · ${values.length}` : ''}</button>
    <dialog ref={dialog} className="commerce-filter-dialog" aria-labelledby="commerce-filter-title" onClick={(event) => { if (event.target === event.currentTarget) dialog.current?.close(); }}>
      <div className="commerce-filter-sheet">
        <header><div><h2 id="commerce-filter-title">商家分类 <small>可多选</small></h2><p>按经营大类查找，选择后点击完成</p></div><button type="button" aria-label="关闭分类" onClick={() => dialog.current?.close()}><X size={20} /></button></header>
        <label className="commerce-filter-search"><Search size={16} /><input value={query} aria-label="搜索商家类型" placeholder="搜索分类名称" onChange={(event) => { setQuery(event.target.value); content.current?.scrollTo({ top: 0 }); }} /></label>
        <div className="commerce-filter-body">
          <nav className="commerce-category-rail" aria-label="经营大类">
            <button type="button" className={keyword || !category ? 'active' : ''} aria-pressed={Boolean(keyword) || !category} onClick={() => changeCategory('')}>全部分类</button>
            {groups.map((group) => <button key={group.code} type="button" className={!keyword && category === group.code ? 'active' : ''} aria-pressed={!keyword && category === group.code} onClick={() => changeCategory(group.code)}><span>{group.name}</span>{group.types.some((type) => values.includes(type.typeCode)) ? <i aria-label="包含已选类型" /> : null}</button>)}
          </nav>
          <div className="commerce-category-content" ref={content}>
            {!keyword && !category ? <button type="button" className={`commerce-all-types ${!values.length ? 'active' : ''}`} aria-pressed={!values.length} onClick={() => onChange([])}>全部商家{!values.length ? <Check size={14} /> : null}</button> : null}
            {visibleGroups.map((group) => <section key={group.code} className="commerce-category-section" aria-label={group.name}>
              <h3>{group.name}<small>{group.types.length} 类</small></h3>
              <div className="commerce-filter-options" role="group" aria-label={`${group.name}商家类型`}>
                {group.types.map((type) => <button key={type.typeCode} type="button" className={values.includes(type.typeCode) ? 'active' : ''} aria-pressed={values.includes(type.typeCode)} onClick={() => choose(type.typeCode)}><span>{type.name}</span><span className="commerce-type-check" aria-hidden="true">{values.includes(type.typeCode) ? <Check size={11} /> : null}</span></button>)}
              </div>
            </section>)}
            {!visibleGroups.length ? <p className="commerce-filter-empty" role="status">{keyword ? '暂无匹配分类' : '暂无可用分类'}</p> : null}
          </div>
        </div>
        <footer className="commerce-filter-actions"><button className="commerce-filter-clear" type="button" disabled={!values.length} onClick={() => onChange([])}>清空</button><span aria-live="polite">{values.length ? `已选 ${values.length} 项` : '全部商家'}</span><button className="commerce-filter-done" type="button" onClick={() => dialog.current?.close()}>完成{values.length ? `（${values.length}）` : ''}</button></footer>
      </div>
    </dialog>
  </div>;
}
