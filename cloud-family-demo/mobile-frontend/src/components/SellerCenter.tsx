import { ProductTypePicker } from './ProductTypePicker';
import { useEffect, useRef, useState } from 'react';
import { Plus, Package, Trash2, X } from 'lucide-react';
import { deleteMyProduct, loadMyProducts, loadMyShop, loadMerchantTypes, saveMyProduct, saveMyShop } from '../services/commerceService';
import type { Product, ProductInput, Shop, MerchantType } from '../services/commerceService';
import { CommerceCover, CommerceHeader, CommercePager, commerceError, productStatusLabel } from './CommerceShared';

const emptyProduct: ProductInput = { title: '', typeCode: '', category: '', coverUrl: '', summary: '', content: '', price: '', stock: '0', sortOrder: '0', status: 'DRAFT' };

export function SellerCenter({ onBack, initialStatus = '' }: { onBack: () => void; initialStatus?: string }) {
  const [shop, setShop] = useState<Shop | null>(null);
  const [shopEditing, setShopEditing] = useState(false);
  const [shopValues, setShopValues] = useState({ name: '', coverUrl: '', description: '', contact: '' });
  const [merchantTypes, setMerchantTypes] = useState<MerchantType[]>([]);
  const [selectedTypeCodes, setSelectedTypeCodes] = useState<string[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState(initialStatus);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);
  const [editor, setEditor] = useState<{ id: number | null; values: ProductInput } | null>(null);
  const [deleting, setDeleting] = useState<Product | null>(null);
  const cancelDeleteRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    let active = true;
    setLoading(true); setError(null);
    Promise.all([loadMyShop(), loadMyProducts(page, '', status), loadMerchantTypes()]).then(([nextShop, result, nextTypes]) => {
      if (!active) return;
      setMerchantTypes(nextTypes); setSelectedTypeCodes(nextShop?.typeCodes || []);
      setShop(nextShop); setProducts(result.records || []); setTotal(result.total);
      if (nextShop) setShopValues({ name: nextShop.name, coverUrl: nextShop.coverUrl || '', description: nextShop.description, contact: nextShop.contact || '' });
      const last = Math.max(1, Math.ceil(result.total / 12)); if (page > last) setPage(last);
    }).catch((err: unknown) => { if (active) setError(commerceError(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, status, retry]);

  useEffect(() => { if (deleting) cancelDeleteRef.current?.focus(); }, [deleting]);

  async function act(action: () => Promise<unknown>, message: string) {
    if (savingRef.current) return;
    savingRef.current = true; setSaving(true); setError(null); setSuccess(null);
    try { await action(); setSuccess(message); setEditor(null); setDeleting(null); setShopEditing(false); setRetry((value) => value + 1); }
    catch (err) { setError(commerceError(err)); }
    finally { savingRef.current = false; setSaving(false); }
  }

  const allowedTypes=(shop?.types || []).filter((type)=>type.status==='ENABLED');
  const editingProduct = products.find((product) => product.id === editor?.id);
  const moderationReason = editingProduct?.moderationReason || '具体原因请联系管理员';
  const publicationBlocked = shop?.status !== 'ACTIVE' || products.some((product) => product.id === editor?.id && product.moderationStatus === 'BLOCKED');

  function edit(product: Product) {
    setError(null); setSuccess(null);
    const values = Object.fromEntries(Object.keys(emptyProduct).map((key) => [key, String((product as unknown as Record<string, unknown>)[key] ?? '')]));
    if (product.moderationStatus === 'BLOCKED' || shop?.status === 'BLOCKED') values.status = 'OFFLINE';
    setEditor({ id: product.id, values });
  }

  if (editor) return <div className="tool-screen seller-screen">
    <CommerceHeader title={editor.id === null ? '新增商品' : '修改商品'} onBack={() => { if (!saving) { setEditor(null); setError(null); } }} />
    <form className="seller-form" onSubmit={(event) => { event.preventDefault(); if(!allowedTypes.some((type)=>type.typeCode===editor.values.typeCode)) {setError('请选择商家已选择且启用的商品类型');return;} if (publicationBlocked && editor.values.status === 'PUBLISHED') { setError('商品或店铺已被后台停用，不能上架，请联系管理员'); return; } void act(() => saveMyProduct(editor.id, editor.values), '商品保存成功'); }}>
      <label>商品名称<input required maxLength={100} value={editor.values.title} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, title: event.target.value } })} /></label>
      <ProductTypePicker options={allowedTypes} value={editor.values.typeCode} disabled={saving}
        onChange={(typeCode)=>setEditor({...editor,values:{...editor.values,typeCode}})} />
      <label>封面图片地址<input maxLength={1024} placeholder="https://… 或站内图片路径（可选）" value={editor.values.coverUrl} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, coverUrl: event.target.value } })} /></label>
      <div className="seller-form-row">
        <label>价格（元）<input required type="number" min="0" max="9999999999.99" step="0.01" value={editor.values.price} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, price: event.target.value } })} /></label>
        <label>库存<input required type="number" min="0" max="2147483647" step="1" value={editor.values.stock} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, stock: event.target.value } })} /></label>
      </div>
      <div className="seller-form-row">
        <label>排序<input required type="number" min="0" max="2147483647" step="1" value={editor.values.sortOrder} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, sortOrder: event.target.value } })} /></label>
        <label>上架状态<select value={editor.values.status} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, status: event.target.value } })}><option value="DRAFT">草稿</option><option value="PUBLISHED" disabled={publicationBlocked}>上架</option><option value="OFFLINE">下架</option></select></label>
      </div>
      {publicationBlocked ? <div className="seller-moderation-notice" role="status"><strong>系统停用</strong>
        {editingProduct?.moderationStatus === 'BLOCKED' ? <p>停用原因：{moderationReason}</p> : null}
        {shop?.status === 'PENDING' ? <p className="settings-info-card" role="status">商家申请待审核，审核通过后才会公开展示并允许添加商品。</p> : null}
      {shop?.status === 'REJECTED' ? <p className="settings-message error">商家申请已驳回，请修改资料后重新提交申请。</p> : null}
      {shop?.status === 'BLOCKED' ? <p>所属店铺已被系统停用，商品暂不可上架，请联系管理员。</p> : null}
        <small>仍可查看和修改商品资料；恢复上架需联系管理员。</small>
      </div> : null}
      <label>商品简介<textarea required maxLength={300} rows={3} value={editor.values.summary} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, summary: event.target.value } })} /></label>
      <label>商品详情<textarea required maxLength={100000} rows={7} value={editor.values.content} onChange={(event) => setEditor({ ...editor, values: { ...editor.values, content: event.target.value } })} /></label>
      {error ? <p className="settings-message error" role="alert">{error}</p> : null}
      <div className="seller-actions"><button type="button" disabled={saving} onClick={() => { setEditor(null); setError(null); }}>取消</button><button className="seller-primary" disabled={saving} type="submit">{saving ? '保存中…' : '保存商品'}</button></div>
    </form>
  </div>;

  return <div className="tool-screen seller-screen">
    <CommerceHeader title="我的店铺与商品" onBack={() => { if (!saving) onBack(); }} />
    {loading ? <div className="settings-info-card" role="status">加载中…</div> : null}
    {error ? <div className="settings-message error" role="alert">{error} <button type="button" disabled={saving} onClick={() => setRetry(retry + 1)}>重试</button></div> : null}
    {success ? <p className="seller-success" role="status">{success}</p> : null}
    {!loading ? <>
      {shop?.status === 'PENDING' ? <p className="settings-info-card" role="status">商家申请待审核，审核通过后才会公开展示并允许添加商品。</p> : null}
      {shop?.status === 'REJECTED' ? <p className="settings-message error">商家申请已驳回，请修改资料后重新提交申请。</p> : null}
      {shop?.status === 'BLOCKED' ? <p className="settings-message error">店铺已被后台停用，前台暂不可见。如需恢复请联系管理员。</p> : null}
      <section className="seller-panel">
        <div className="seller-panel-heading"><h2>{shop ? shop.name : '申请成为商家'}</h2>{shop?.status === 'REJECTED' ? <button type="button" disabled={saving} onClick={() => setShopEditing(!shopEditing)}>{shopEditing ? '收起' : '编辑资料'}</button> : null}</div>
        {shop && !shopEditing ? <><p>{shop.description}</p><p>经营类型：{shop.typeNames || '待后台设置'}</p><small>商家ID：{shop.id} · 当前列表 {total} 件</small></> : null}
        {!shop || (shop.status === 'REJECTED' && shopEditing) ? <form className="seller-form" onSubmit={(event) => { event.preventDefault(); if (!selectedTypeCodes.length || selectedTypeCodes.some((code) => !merchantTypes.some((type) => type.typeCode === code))) { setError('请选择至少一个已启用的商家类型'); return; } void act(() => saveMyShop({...shopValues, typeCodes: selectedTypeCodes}), '商家申请已提交，请等待后台审核'); }}>
          <label>店铺名称<input required maxLength={100} value={shopValues.name} onChange={(event) => setShopValues({ ...shopValues, name: event.target.value })} /></label>
          <label>封面图片地址<input maxLength={1024} value={shopValues.coverUrl} onChange={(event) => setShopValues({ ...shopValues, coverUrl: event.target.value })} /></label>
          <label>公开联系方式<input maxLength={100} placeholder="自愿填写，展示给浏览商品的用户" value={shopValues.contact} onChange={(event) => setShopValues({ ...shopValues, contact: event.target.value })} /></label>
          <fieldset className="seller-type-choices" disabled={saving}><legend>申请商家类型（必选，可多选）</legend>
            {[...merchantTypes, ...(shop?.types || []).filter((type) => !merchantTypes.some((item) => item.typeCode === type.typeCode))].map((type) => <label key={type.typeCode}>
              <input type="checkbox" checked={selectedTypeCodes.includes(type.typeCode)} disabled={type.status !== 'ENABLED' && !selectedTypeCodes.includes(type.typeCode)}
                onChange={(event) => setSelectedTypeCodes((codes) => event.target.checked ? [...codes, type.typeCode] : codes.filter((code) => code !== type.typeCode))} />
              {type.name}{type.status !== 'ENABLED' ? '（已停用，请取消）' : ''}
            </label>)}
            {!merchantTypes.length ? <p>暂无可用商家类型，请联系管理员。</p> : null}
            <small>类型由后台维护，申请类型需经后台审核，提交后仅可查看。</small>
          </fieldset>
          <label>店铺介绍<textarea required rows={4} maxLength={2000} value={shopValues.description} onChange={(event) => setShopValues({ ...shopValues, description: event.target.value })} /></label>
          <button className="seller-primary" type="submit" disabled={saving}>{saving ? '保存中…' : !shop || shop.status === 'REJECTED' ? '提交商家申请' : '保存店铺'}</button>
        </form> : null}
      </section>
      {shop ? <section className="seller-products">
        <div className="seller-panel-heading"><h2>我的商品</h2><button className="seller-primary" type="button" disabled={saving || shop.status !== 'ACTIVE' || allowedTypes.length===0} onClick={() => { setError(null); setSuccess(null); setEditor({ id: null, values: { ...emptyProduct } }); }}><Plus size={16} />新增商品</button></div>
        {!allowedTypes.length ? <p className="settings-message error">商家类型由后台管理，暂无可用类型，请联系管理员。</p> : null}
        <div className="seller-status-tabs" role="group" aria-label="商品状态筛选">
          {[['', '全部'], ['PUBLISHED', '已上架'], ['DRAFT', '草稿'], ['OFFLINE', '已下架'], ['SYSTEM_BLOCKED', '系统停用']].map(([value, label]) => (
            <button key={value} type="button" aria-pressed={status === value}
              className={`${status === value ? 'active' : ''} ${value === 'SYSTEM_BLOCKED' ? 'system-blocked' : ''}`}
              disabled={saving} onClick={() => { setPage(1); setStatus(value); }}>{label}</button>
          ))}
        </div>
        {products.length === 0 ? <div className="showcase-empty"><Package size={36} /><p>{status === 'SYSTEM_BLOCKED' ? '暂无系统停用商品' : status ? '该状态下暂无商品' : '暂无商品，点击“新增商品”开始添加'}</p></div> : null}
        {products.map((product) => <article className="seller-product" key={product.id}>
          <CommerceCover url={product.coverUrl} title={product.title} />
          <div><h3>{product.title}</h3><p className="commerce-price">¥{Number(product.price).toFixed(2)}</p><small>{product.moderationStatus === 'BLOCKED' || shop.status === 'BLOCKED' ? '系统停用' : productStatusLabel[product.status]} · 库存 {product.stock}</small>
            {product.moderationStatus === 'BLOCKED' ? <div className="seller-moderation-notice"><strong>系统停用</strong><p>停用原因：{product.moderationReason || '具体原因请联系管理员'}</p></div> : null}
            <div className="seller-actions"><button type="button" disabled={saving} onClick={() => edit(product)}>{product.moderationStatus === 'BLOCKED' ? '查看/修改' : '修改'}</button><button className="seller-danger" type="button" disabled={saving} onClick={() => { setError(null); setDeleting(product); }}><Trash2 size={14} />删除</button></div>
          </div>
        </article>)}
        <CommercePager page={page} total={total} onChange={setPage} />
      </section> : null}
    </> : null}
    {deleting ? <div className="seller-modal-backdrop" onKeyDown={(event) => { if (event.key === 'Escape' && !saving) setDeleting(null); }}>
      <section className="seller-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="seller-delete-title">
        <div className="seller-panel-heading"><h2 id="seller-delete-title">删除商品</h2><button type="button" aria-label="关闭" disabled={saving} onClick={() => setDeleting(null)}><X size={18} /></button></div>
        <p>确认删除“{deleting.title}”？</p><small>删除后无法恢复。</small>
        {error ? <p role="alert" className="settings-message error">{error}</p> : null}
        <div className="seller-actions"><button ref={cancelDeleteRef} type="button" disabled={saving} onClick={() => setDeleting(null)}>取消</button><button className="seller-delete-submit" type="button" disabled={saving} onClick={() => { void act(() => deleteMyProduct(deleting.id), '商品已删除'); }}>{saving ? '删除中…' : '确认删除'}</button></div>
      </section>
    </div> : null}
  </div>;
}
