import { useId, useRef, useState } from 'react';
import { Check, Search, SlidersHorizontal, Store, ChevronRight, X } from 'lucide-react';
import type { MerchantType } from '../services/commerceService';

type Category = { code: string; name: string; order: number; types: MerchantType[] };

export function MerchantTypeFilter({ types, values, onChange, variant = 'filter', disabled = false, historicalTypes = [] }: {
  types: MerchantType[]; values: string[]; onChange: (codes: string[]) => void;
  variant?: 'filter' | 'application'; disabled?: boolean; historicalTypes?: MerchantType[];
}) {
  const application = variant === 'application';
  const titleId = useId();
  const limit = application ? 20 : Infinity;
  const dialog = useRef<HTMLDialogElement>(null);
  const content = useRef<HTMLDivElement>(null);
  const strip = useRef<HTMLDivElement>(null);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const keyword = query.trim().toLowerCase();
  const selected = new Set(values);
  const selectedTypes = types.filter((type) => selected.has(type.typeCode));
  const displaySelectedTypes = application ? values.map((code) => types.find((type) => type.typeCode === code) || historicalTypes.find((type) => type.typeCode === code) || { id: 0, typeCode: code, name: code, status: 'DISABLED' as const }) : selectedTypes;
  const invalid = (code: string) => !types.some((type) => type.typeCode === code && type.status === 'ENABLED');
  const orderedTypes = [...selectedTypes, ...types.filter((type) => !selected.has(type.typeCode))];
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
  function choose(code: string) {
    if (disabled || (!selected.has(code) && (values.length >= limit || invalid(code)))) return;
    onChange(selected.has(code) ? values.filter((value) => value !== code) : [...values, code]);
    strip.current?.scrollTo({ left: 0, behavior: 'smooth' });
  }
  function changeCategory(code: string) { setCategory(code); setQuery(''); content.current?.scrollTo({ top: 0 }); }
  function show() { setQuery(''); setCategory(''); dialog.current?.showModal(); content.current?.scrollTo({ top: 0 }); }
  return <div className={application ? 'merchant-application-selector' : 'commerce-type-toolbar'}>
    {application ? <>
      <div className="merchant-application-heading"><span>按经营范围选择，最多20项</span><strong className={values.length ? 'has-selection' : ''}>{values.length} / 20</strong></div>
      <button className="merchant-application-trigger" type="button" disabled={disabled || !types.length} aria-haspopup="dialog" onClick={show}>
        <span className="merchant-application-icon"><Store size={19} /></span><span><strong>{values.length ? '调整经营类型' : '选择经营类型'}</strong><small>{types.length ? '按大类查找，支持搜索和多选' : '暂无可用类型，请联系管理员'}</small></span><ChevronRight size={17} />
      </button>
      {displaySelectedTypes.length ? <div className="merchant-application-tags" role="group" aria-label="已选申请类型">
        {displaySelectedTypes.slice(0, 6).map((type) => <button type="button" key={type.typeCode} disabled={disabled} className={invalid(type.typeCode) ? 'is-unavailable' : ''} aria-label={`移除申请类型：${type.name}`} onClick={() => choose(type.typeCode)}>{type.name}{invalid(type.typeCode) ? <small>已停用</small> : null}<X size={12} aria-hidden="true" /></button>)}
        {displaySelectedTypes.length > 6 ? <button className="merchant-application-more" type="button" disabled={disabled} onClick={show}>查看全部 {displaySelectedTypes.length} 项</button> : null}
      </div> : <p className="merchant-application-empty">请选择至少一个与店铺业务相符的类型</p>}
      {values.some(invalid) ? <p className="merchant-application-warning" role="alert">已停用或失效类型不可申请，请先移除。</p> : null}
      <p className="merchant-application-hint">类型由后台审核确认，申请提交后仅可查看。</p>
    </> : <>
    <div className="showcase-categories" ref={strip} role="group" aria-label="商家类型筛选">
      <button type="button" className={!values.length ? 'active' : ''} aria-pressed={!values.length} onClick={() => onChange([])}>全部</button>
      {orderedTypes.map((type) => <button type="button" key={type.typeCode} className={selected.has(type.typeCode) ? 'active commerce-selected-chip' : ''} aria-pressed={selected.has(type.typeCode)} aria-label={selected.has(type.typeCode) ? `取消筛选：${type.name}` : type.name} onClick={() => choose(type.typeCode)}>{type.name}{selected.has(type.typeCode) ? <X size={12} aria-hidden="true" /> : null}</button>)}
    </div>
    <button className="commerce-filter-trigger" type="button" aria-haspopup="dialog" onClick={show}><SlidersHorizontal size={15} />分类{values.length ? ` · ${values.length}` : ''}</button>
    </>}
    <dialog ref={dialog} className="commerce-filter-dialog" aria-labelledby={titleId} onKeyDown={(event) => { if (event.key === 'Enter' && event.target instanceof HTMLInputElement) event.preventDefault(); }} onClick={(event) => { if (event.target === event.currentTarget) dialog.current?.close(); }}>
      <div className="commerce-filter-sheet">
        <header><div><h2 id={titleId}>{application ? '选择经营类型' : '商家分类'} <small>可多选</small></h2><p>{application ? '选择实际经营范围，最多20项' : '按经营大类查找，选择后点击完成'}</p></div><button type="button" aria-label="关闭分类" onClick={() => dialog.current?.close()}><X size={20} /></button></header>
        <label className="commerce-filter-search"><Search size={16} /><input value={query} aria-label="搜索商家类型" placeholder="搜索分类名称" onChange={(event) => { setQuery(event.target.value); content.current?.scrollTo({ top: 0 }); }} /></label>
        {displaySelectedTypes.length ? <div className="commerce-selected-types" role="group" aria-label="已选分类">
          <span>已选</span><div>{displaySelectedTypes.map((type) => <button key={type.typeCode} type="button" aria-label={`取消选择：${type.name}`} onClick={() => choose(type.typeCode)}>{type.name}<X size={12} aria-hidden="true" /></button>)}</div>
        </div> : null}
        <div className="commerce-filter-body">
          <nav className="commerce-category-rail" aria-label="经营大类">
            <button type="button" className={keyword || !category ? 'active' : ''} aria-pressed={Boolean(keyword) || !category} onClick={() => changeCategory('')}>全部分类</button>
            {groups.map((group) => <button key={group.code} type="button" className={!keyword && category === group.code ? 'active' : ''} aria-pressed={!keyword && category === group.code} onClick={() => changeCategory(group.code)}><span>{group.name}</span>{group.types.some((type) => values.includes(type.typeCode)) ? <i aria-label="包含已选类型" /> : null}</button>)}
          </nav>
          <div className="commerce-category-content" ref={content}>
            {!application && !keyword && !category ? <button type="button" className={`commerce-all-types ${!values.length ? 'active' : ''}`} aria-pressed={!values.length} onClick={() => onChange([])}>全部商家{!values.length ? <Check size={14} /> : null}</button> : null}
            {visibleGroups.map((group) => <section key={group.code} className="commerce-category-section" aria-label={group.name}>
              <h3>{group.name}<small>{group.types.length} 类</small></h3>
              <div className="commerce-filter-options" role="group" aria-label={`${group.name}商家类型`}>
                {group.types.map((type) => <button key={type.typeCode} type="button" className={values.includes(type.typeCode) ? 'active' : ''} aria-pressed={values.includes(type.typeCode)} disabled={disabled || (!selected.has(type.typeCode) && values.length >= limit)} onClick={() => choose(type.typeCode)}><span>{type.name}</span><span className="commerce-type-check" aria-hidden="true">{values.includes(type.typeCode) ? <Check size={11} /> : null}</span></button>)}
              </div>
            </section>)}
            {!visibleGroups.length ? <p className="commerce-filter-empty" role="status">{keyword ? '暂无匹配分类' : '暂无可用分类'}</p> : null}
          </div>
        </div>
        <footer className="commerce-filter-actions"><button className="commerce-filter-clear" type="button" disabled={!values.length} onClick={() => onChange([])}>清空</button><span aria-live="polite">{application ? `已选 ${values.length} / 20 项` : values.length ? `已选 ${values.length} 项` : '全部商家'}</span><button className="commerce-filter-done" type="button" onClick={() => dialog.current?.close()}>完成{values.length ? `（${values.length}）` : ''}</button></footer>
      </div>
    </dialog>
  </div>;
}
