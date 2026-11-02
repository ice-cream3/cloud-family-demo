import { useEffect, useState } from 'react';
import { ChevronRight, Search, Store, Package } from 'lucide-react';
import { loadShops, loadShop, loadProducts, loadProductCategories, loadProduct } from '../services/commerceService';
import type { Shop, Product } from '../services/commerceService';
import { CommerceCover, CommerceHeader, CommercePager, commerceError } from './CommerceShared';
import { SellerCenter } from './SellerCenter';

export function ShowcaseScreen({ onBack }: { onBack: () => void }) {
  const [seller, setSeller] = useState(false);
  const [shopId, setShopId] = useState<number | null>(null);
  const [productId, setProductId] = useState<number | null>(null);
  const [shop, setShop] = useState<Shop | null>(null);
  const [shops, setShops] = useState<Shop[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [detail, setDetail] = useState<Product | null>(null);
  const [categories, setCategories] = useState<string[]>([]);
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (seller) return;
    let active = true; setLoading(true); setError(null);
    async function load() {
      if (productId !== null) {
        const item = await loadProduct(productId); if (active) setDetail(item);
      } else if (shopId !== null) {
        const [nextShop, nextCategories, result] = await Promise.all([loadShop(shopId), loadProductCategories(shopId), loadProducts(shopId, keyword, category, page)]);
        if (!active) return;
        setShop(nextShop); setCategories(nextCategories); setProducts(result.records || []); setTotal(result.total);
        const last = Math.max(1, Math.ceil(result.total / 12)); if (page > last) setPage(last);
      } else {
        const result = await loadShops(keyword, page); if (!active) return;
        setShops(result.records || []); setTotal(result.total);
        const last = Math.max(1, Math.ceil(result.total / 12)); if (page > last) setPage(last);
      }
    }
    void load().catch((err: unknown) => { if (active) setError(commerceError(err)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [seller, shopId, productId, keyword, category, page, retry]);

  function back() {
    setError(null);
    if (productId !== null) { setProductId(null); setDetail(null); }
    else if (shopId !== null) { setShopId(null); setShop(null); setPage(1); setCategory(''); setKeyword(''); setSearch(''); }
    else onBack();
  }
  if (seller) return <SellerCenter onBack={() => setSeller(false)} />;

  return <div className="tool-screen showcase-screen">
    <CommerceHeader title={productId !== null ? '商品详情' : shopId !== null ? '商家商品' : '精品展示'} onBack={back} />
    {shopId === null ? <>
      <section className="showcase-banner"><Store size={30} /><div><h2>发现身边的精品商家</h2><p>先选商家，再逛好物</p></div></section>
      <button className="seller-entry" type="button" onClick={() => setSeller(true)}><Store size={20} /><span><strong>我的店铺与商品</strong><small>创建店铺，发布和管理自己的商品</small></span><ChevronRight size={18} /></button>
    </> : null}
    {productId === null ? <>
      {shopId !== null && shop ? <section className="commerce-shop-info"><h2>{shop.name}</h2><p>{shop.description}</p>{shop.contact ? <small>联系方式：{shop.contact}</small> : null}</section> : null}
      <form className="tool-search" onSubmit={(event) => { event.preventDefault(); setPage(1); setKeyword(search.trim()); }}>
        <input aria-label={shopId === null ? '搜索商家' : '搜索商品'} value={search} maxLength={100} placeholder={shopId === null ? '搜索商家名称或介绍' : '搜索商品名称或简介'} onChange={(event) => setSearch(event.target.value)} />
        <button type="submit" aria-label="搜索"><Search size={18} /></button>
      </form>
      {shopId !== null ? <div className="showcase-categories" aria-label="商品分类"><button type="button" className={!category ? 'active' : ''} onClick={() => { setCategory(''); setPage(1); }}>全部</button>{Array.from(new Set([...categories, ...(category ? [category] : [])])).map((name) => <button type="button" key={name} className={category === name ? 'active' : ''} onClick={() => { setCategory(name); setPage(1); }}>{name}</button>)}</div> : null}
    </> : null}
    {loading ? <div className="settings-info-card" role="status">加载中…</div> : null}
    {error ? <div className="settings-message error" role="alert">{error} <button type="button" onClick={() => setRetry(retry + 1)}>重试</button></div> : null}
    {!loading && !error && productId === null ? <>
      {(shopId === null ? shops.length : products.length) === 0 ? <div className="showcase-empty"><Package size={40} /><h2>{keyword || category ? '暂无匹配内容' : shopId === null ? '暂无商家' : '商家暂无上架商品'}</h2><p>{shopId === null ? '可通过“我的店铺与商品”创建店铺' : '稍后再来看看吧'}</p></div> : null}
      <div className="showcase-grid">
        {shopId === null ? shops.map((item) => <button className="showcase-card" type="button" key={item.id} onClick={() => { setLoading(true); setShopId(item.id); setPage(1); setSearch(''); setKeyword(''); }}>
          <CommerceCover url={item.coverUrl} title={item.name} /><div className="showcase-card-body"><h2>{item.name}</h2><p>{item.description}</p><span className="showcase-read">{item.productCount || 0} 件商品 · 进店逛逛 <ChevronRight size={14} /></span></div>
        </button>) : products.map((item) => <button className="showcase-card" type="button" key={item.id} onClick={() => { setLoading(true); setProductId(item.id); }}>
          <CommerceCover url={item.coverUrl} title={item.title} /><div className="showcase-card-body"><span className="showcase-category">{item.category}</span><h2>{item.title}</h2><p>{item.summary}</p><div className="commerce-price">¥{Number(item.price).toFixed(2)} <small>{item.stock > 0 ? `库存 ${item.stock}` : '已售罄'}</small></div></div>
        </button>)}
      </div>
      <CommercePager page={page} total={total} onChange={setPage} />
    </> : null}
    {!loading && !error && productId !== null && detail ? <article className="showcase-detail">
      <CommerceCover url={detail.coverUrl} title={detail.title} /><div className="showcase-detail-body"><span className="showcase-category">{detail.category}</span><h2>{detail.title}</h2>
        <div className="commerce-price">¥{Number(detail.price).toFixed(2)} <small>{detail.stock > 0 ? `库存 ${detail.stock}` : '已售罄'}</small></div>
        <p className="commerce-merchant">商家：{detail.shopName}{detail.contact ? ` · 联系方式：${detail.contact}` : ''}</p>
        <p className="showcase-summary">{detail.summary}</p><div className="showcase-content">{detail.content}</div>
      </div>
    </article> : null}
  </div>;
}
