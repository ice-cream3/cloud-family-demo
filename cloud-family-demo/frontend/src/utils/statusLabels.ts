/** Only presentation labels; request payloads and stored status codes stay unchanged. */
export type StatusContext = 'default' | 'shop' | 'product' | 'moderation' | 'membership' | 'reservation' | 'service';

const labels: Record<string, string> = {
  ENABLED: '启用', DISABLED: '停用', ACTIVE: '生效中', INACTIVE: '未生效',
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', BLOCKED: '已停用',
  DRAFT: '草稿', PUBLISHED: '已上架', OFFLINE: '已下架', SYSTEM_BLOCKED: '系统停用',
  OPEN: '可预约', BOOKED: '已预约', NO_SHOW: '未履约', EXPIRED: '已过期', SUSPENDED: '已暂停',
  CANCELLED: '已取消', CANCELED: '已取消', CLOSED: '已关闭',
  SUCCESS: '成功', FAILED: '失败', FAILURE: '失败', PROCESSING: '处理中', RUNNING: '运行中',
  COMPLETED: '已完成', LOCKED: '已锁定', DELETED: '已删除',
  UP: '运行正常', DOWN: '服务异常', OUT_OF_SERVICE: '停止服务', UNKNOWN: '状态未知', ONLINE: '在线',
};
const contextualLabels: Partial<Record<StatusContext, Record<string, string>>> = {
  shop: { ACTIVE: '审核通过' },
  moderation: { ACTIVE: '正常', BLOCKED: '系统停用' },
  reservation: { PENDING: '预约中' },
};

export function statusLabel(value: unknown, context: StatusContext = 'default'): string {
  if (value === null || value === undefined || String(value).trim() === '') return '-';
  const text = String(value).trim();
  if (text === '-') return text;
  const code = text.replace(/^"(.*)"$/, '$1').toUpperCase();
  return contextualLabels[context]?.[code] || labels[code] || (/[\u3400-\u9fff]/.test(text) ? text : '未知状态');
}

export function isStatusField(name: string): boolean {
  return /(?:^|_)(?:status|state)$|(?:Status|State)$/.test(name);
}
