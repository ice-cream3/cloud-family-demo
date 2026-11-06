import { useState } from 'react';
import type { ReactNode } from 'react';
import { ChevronLeft, Store } from 'lucide-react';
export function commerceError(error: unknown) { return error instanceof Error ? error.message : '操作失败，请稍后重试'; }
export function CommerceCover({ url, title }: { url?: string | null; title: string }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  return url && failedUrl !== url
    ? <img className="showcase-cover" src={url} alt={title} loading="lazy" onError={() => setFailedUrl(url)} />
    : <div className="showcase-cover showcase-cover-placeholder" aria-hidden="true"><Store size={36} /></div>;
}
export function CommerceHeader({ title, onBack, action }: { title: string; onBack: () => void; action?: ReactNode }) {
  return <header className="tool-detail-head"><button type="button" onClick={onBack} aria-label="返回"><ChevronLeft size={22} /></button><h1>{title}</h1>{action || <span />}</header>;
}
export function CommercePager({ page, total, onChange }: { page: number; total: number; onChange: (page: number) => void }) {
  return total > 12 ? <div className="showcase-pagination"><button type="button" disabled={page <= 1} onClick={() => onChange(page - 1)}>上一页</button><span>{page} / {Math.max(1, Math.ceil(total / 12))}</span><button type="button" disabled={page * 12 >= total} onClick={() => onChange(page + 1)}>下一页</button></div> : null;
}
export const productStatusLabel = { DRAFT: '草稿', PUBLISHED: '已上架', OFFLINE: '已下架' };
