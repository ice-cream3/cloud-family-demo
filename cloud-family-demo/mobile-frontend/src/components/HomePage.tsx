import { useEffect, useState } from 'react';
import type { FocusEvent, FormEvent } from 'react';
import {
  Archive,
  Bell,
  Calculator,
  CalendarDays,
  CheckCircle2,
  Camera,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Crown,
  FileImage,
  FileText,
  Gift,
  Grid2X2,
  Home,
  Image,
  Images,
  LogOut,
  MessageCircle,
  PackageCheck,
  Palette,
  RefreshCw,
  ScanLine,
  Search,
  Settings,
  ShieldCheck,
  Sparkles,
  Star,
  UserCheck,
  UserRound,
  Wand2,
  Wrench,
  X,
  Zap,
} from 'lucide-react';
import type { AppDocument, FavoriteItem, Membership, PartnerHealth, ProfileDynamic, TripOwner, TripPublishRequest, TripSlot, UserHistoryItem, UserProfile } from '../types/api';
import type { AccountSecurity } from '../types/api';
import {
  changeAccountPassword,
  loadAccountSecurity,
  loadAppDocument,
  loadAppVersion,
  loadFavorites,
  loadHistory,
  loadMembership,
  loadNotificationSettings,
  loadBookableTripSlots,
  loadMyTrips,
  loadMyTripReservations,
  loadTripOwners,
  publishTrip,
  refreshProfileDynamic,
  applyTrip,
  reviewTrip,
  updateAccountSecurity,
  updateNotificationSettings,
} from '../services/partnerService';
import { ApiError } from '../services/apiClient';
import type { Session } from '../services/tokenStore';

type HomePageProps = {
  session: Session;
  profile: UserProfile | null;
  health: PartnerHealth | null;
  loading: boolean;
  error: string | null;
  onReload: () => void;
  onLogout: () => void;
};

type TabKey = 'home' | 'tools' | 'activity' | 'message' | 'profile';
type ScreenKey = TabKey | 'compress' | 'history' | 'settings' | 'membership' | 'favorites' | 'trips' | 'myTrips';
type SettingDetail = 'account' | 'notifications' | 'about' | 'privacy' | 'agreement' | 'version';
type SearchableTool = {
  icon: typeof Image;
  label: string;
  tone: string;
  screen?: 'compress';
};

const TRIP_CONFLICT_CODE = 200002;

const mainTools = [
  { icon: Image, label: '图片处理', tone: 'green' },
  { icon: FileText, label: '文件转换', tone: 'orange' },
  { icon: ScanLine, label: '扫描识别', tone: 'purple' },
  { icon: PackageCheck, label: '生活工具', tone: 'pink' },
];

const imageTools = [
  { icon: Image, label: '图片压缩', tone: 'green', screen: 'compress' as const },
  { icon: Wand2, label: '图片裁剪', tone: 'blue' },
  { icon: Archive, label: '图片格式转换', tone: 'orange' },
  { icon: Palette, label: '图片加水印', tone: 'purple' },
  { icon: FileImage, label: '图片转PDF', tone: 'red' },
  { icon: Images, label: '拼图', tone: 'blue' },
  { icon: Camera, label: '图片尺寸调整', tone: 'sky' },
  { icon: Sparkles, label: '图片修复', tone: 'green' },
];

const fileTools = [
  { icon: FileText, label: 'PDF转图片', tone: 'orange' },
  { icon: FileImage, label: '图片转PDF', tone: 'red' },
  { icon: FileText, label: 'Word转PDF', tone: 'blue' },
  { icon: FileText, label: 'PDF转Word', tone: 'indigo' },
  { icon: FileText, label: 'Excel转PDF', tone: 'green' },
  { icon: FileText, label: 'PPT转PDF', tone: 'orange' },
  { icon: FileText, label: 'TXT转PDF', tone: 'blue' },
  { icon: Archive, label: 'PDF合并', tone: 'red' },
];

const moreTools = [
  { icon: Calculator, label: '计算器', tone: 'blue' },
  { icon: Zap, label: '汇率换算', tone: 'green' },
  { icon: Wrench, label: '单位换算', tone: 'indigo' },
  { icon: FileText, label: '日期计算', tone: 'orange' },
  { icon: Gift, label: '随机数', tone: 'orange' },
  { icon: ShieldCheck, label: '密码生成器', tone: 'blue' },
  { icon: Palette, label: '颜色取值', tone: 'sky' },
  { icon: Camera, label: '屏幕录制', tone: 'red' },
  { icon: Bell, label: '音乐转换', tone: 'purple' },
  { icon: FileText, label: '视频转换', tone: 'blue' },
  { icon: MessageCircle, label: '网速测试', tone: 'green' },
  { icon: Wrench, label: '尺子测量', tone: 'purple' },
];

const activityCards = [
  { tag: '限时优惠', title: '年度会员 立减50%', desc: '高级工具无限用', action: '立即抢购', tone: 'gold' },
  { tag: '邀请好友', title: '邀请好友得7天会员', desc: '多邀多得，最高送90天', action: '去邀请', tone: 'warm' },
  { tag: '新功能上线', title: 'AI 图片修复', desc: '智能修复模糊图片', action: '立即体验', tone: 'purple' },
  { tag: '学生专享', title: '学生认证享5折', desc: '1分钟完成认证', action: '去认证', tone: 'green' },
];

const messages = [
  { icon: ShieldCheck, title: '系统通知', desc: '欢迎使用我们的工具 App!', time: '1小时前', tone: 'blue' },
  { icon: Star, title: '会员活动', desc: '年度会员限时5折，快来抢购!', time: '3小时前', tone: 'gold' },
  { icon: PackageCheck, title: '任务完成', desc: '图片压缩已完成，共节省 2.4MB', time: '5小时前', tone: 'green' },
  { icon: FileImage, title: '功能更新', desc: '新增 AI 图片修复功能', time: '1天前', tone: 'purple' },
  { icon: Gift, title: '订单通知', desc: '你的会员订单已支付成功', time: '2天前', tone: 'red' },
  { icon: MessageCircle, title: '客服消息', desc: '您好，有什么可以帮助您?', time: '2天前', tone: 'blue' },
];

const tripStartMinute = 0;
const tripEndMinute = 23 * 60 + 50;
const tripDurationLimitMinutes = 4 * 60;

export function HomePage({ session, profile, health, loading, error, onReload, onLogout }: HomePageProps) {
  const [screen, setScreen] = useState<ScreenKey>('home');
  const [showMoreTools, setShowMoreTools] = useState(false);
  const [dynamic, setDynamic] = useState<ProfileDynamic | null>(null);
  const [dynamicLoading, setDynamicLoading] = useState(false);
  const displayName = profile?.displayName || session.username || '用户名';
  const serviceStatus = dynamic?.serviceStatus || health?.status || (profile ? 'UP' : 'UNKNOWN');
  const activeTab = ['history', 'settings', 'membership', 'favorites', 'trips', 'myTrips'].includes(screen) ? 'profile' : screen === 'compress' ? 'tools' : screen as TabKey;

  useEffect(() => {
    void loadDynamic();
  }, []);

  async function loadDynamic() {
    setDynamicLoading(true);
    try {
      setDynamic(await refreshProfileDynamic());
    } finally {
      setDynamicLoading(false);
    }
  }

  async function refreshDynamic() {
    onReload();
    await loadDynamic();
  }

  function setTab(tab: TabKey) {
    setScreen(tab);
    setShowMoreTools(false);
  }

  return (
    <main className="tool-app-shell">
      <section className="tool-phone" aria-label="移动端工具 App">
        <StatusBar />
        {error ? <div className="tool-error">{error}</div> : null}

        {screen === 'home' ? (
          <HomeScreen
            displayName={displayName}
            onOpenTools={() => setTab('tools')}
            onOpenCompress={() => setScreen('compress')}
            onOpenTrips={() => setScreen('trips')}
          />
        ) : null}
        {screen === 'tools' ? (
          <ToolsScreen onOpenCompress={() => setScreen('compress')} onOpenMore={() => setShowMoreTools(true)} />
        ) : null}
        {screen === 'compress' ? (
          <CompressScreen onBack={() => setScreen('tools')} />
        ) : null}
        {screen === 'activity' ? <ActivityScreen /> : null}
        {screen === 'message' ? <MessageScreen /> : null}
        {screen === 'profile' ? (
          <ProfileScreen
            displayName={displayName}
            serviceStatus={serviceStatus}
            loading={loading || dynamicLoading}
            dynamic={dynamic}
            onReload={refreshDynamic}
            onLogout={onLogout}
            onOpenMembership={() => setScreen('membership')}
            onOpenHistory={() => setScreen('history')}
            onOpenFavorites={() => setScreen('favorites')}
            onOpenTrips={() => setScreen('myTrips')}
            onOpenSettings={() => setScreen('settings')}
          />
        ) : null}
        {screen === 'history' ? <HistoryScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'membership' ? <MembershipScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'favorites' ? <FavoritesScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'trips' ? (
          <TripScheduleScreen
            mode="booking"
            displayName={displayName}
            username={session.username || ''}
            onBack={() => setScreen('home')}
            onOpenMyTrips={() => setScreen('myTrips')}
          />
        ) : null}
        {screen === 'myTrips' ? (
          <TripScheduleScreen
            mode="mine"
            displayName={displayName}
            username={session.username || ''}
            onBack={() => setScreen('profile')}
            onOpenBooking={() => setScreen('trips')}
          />
        ) : null}
        {screen === 'settings' ? <SettingsScreen displayName={displayName} onBack={() => setScreen('profile')} onLogout={onLogout} /> : null}

        <BottomTabs active={activeTab} onChange={setTab} hasMessageDot />
        {showMoreTools ? <MoreToolsSheet onClose={() => setShowMoreTools(false)} /> : null}
      </section>
    </main>
  );
}

function StatusBar() {
  return (
    <div className="tool-status-bar" aria-hidden="true">
      <span>9:41</span>
      <i />
      <span>▮▮▮ ︎⌁ ▰</span>
    </div>
  );
}

function HomeScreen({
  displayName,
  onOpenTools,
  onOpenCompress,
  onOpenTrips,
}: {
  displayName: string;
  onOpenTools: () => void;
  onOpenCompress: () => void;
  onOpenTrips: () => void;
}) {
  const [searchText, setSearchText] = useState('');
  const [searchActive, setSearchActive] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const searchResults = searchTools(searchText);

  function openTool(item: SearchableTool) {
    if (item.screen === 'compress') {
      onOpenCompress();
      return;
    }
    showToast(`${item.label}功能建设中`);
  }

  function submitSearch() {
    const firstResult = searchResults[0];
    if (!searchText.trim()) {
      setSearchActive(true);
      return;
    }
    if (firstResult) {
      openTool(firstResult);
      return;
    }
    showToast('暂无匹配工具');
  }

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(null), 1600);
  }

  return (
    <div className="tool-screen">
      <header className="tool-home-head">
        <div className="tool-avatar">
          <Sparkles size={20} />
        </div>
        <div>
          <strong>Hi，早上好!</strong>
          <span>{displayName} 的效率工具箱 ✨</span>
        </div>
        <button type="button" aria-label="快速打开图片压缩" onClick={onOpenCompress}>
          <Zap size={16} />
        </button>
      </header>

      <div className={`tool-search-wrap ${searchActive ? 'active' : ''}`}>
        <form className="tool-search" onSubmit={(event) => {
          event.preventDefault();
          submitSearch();
        }}>
          <input
            value={searchText}
            placeholder="搜索工具，试试“图片压缩”"
            onChange={(event) => {
              setSearchText(event.target.value);
              setSearchActive(true);
            }}
            onFocus={() => setSearchActive(true)}
          />
          <button type="submit" aria-label="搜索工具">
            <Search size={18} />
          </button>
        </form>
        {searchActive ? (
          <div className="tool-search-results">
            {searchResults.length > 0 ? searchResults.slice(0, 5).map((item) => {
              const Icon = item.icon;
              return (
                <button key={item.label} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => openTool(item)}>
                  <span className={`tool-icon ${item.tone}`}>
                    <Icon size={18} />
                  </span>
                  <span>
                    <strong>{item.label}</strong>
                    <small>{toolCategoryLabel(item)}</small>
                  </span>
                  <ChevronRight size={16} />
                </button>
              );
            }) : <div className="tool-search-empty">暂无匹配工具</div>}
          </div>
        ) : null}
      </div>

      <section className="tool-home-banner">
        <div>
          <h1>高效工具<br />让生活更简单</h1>
          <p>多种实用工具，随时随地使用</p>
        </div>
        <button type="button" aria-label="进入工具" onClick={onOpenTools}>
          <ChevronRight size={20} />
        </button>
      </section>

      <div className="tool-shortcut-grid">
        <button type="button" onClick={onOpenTrips}>
          <span className="tool-icon blue">
            <CalendarDays size={24} />
          </span>
          <em>行程预约</em>
        </button>
        {mainTools.map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" onClick={item.label === '图片处理' ? onOpenTools : undefined}>
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={24} />
              </span>
              <em>{item.label}</em>
            </button>
          );
        })}
      </div>

      <SectionTitle title="最近使用" action="查看全部" />
      <div className="tool-recent-grid">
        {imageTools.slice(0, 4).map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" onClick={item.screen ? onOpenCompress : undefined}>
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={22} />
              </span>
              <em>{item.label}</em>
            </button>
          );
        })}
      </div>

      <SectionTitle title="推荐工具" />
      <button className="tool-recommend" type="button">
        <span className="tool-icon purple">
          <FileImage size={24} />
        </span>
        <span>
          <strong>图片转PDF</strong>
          <small>多张图片一键合并为PDF</small>
        </span>
        <ChevronRight size={18} />
      </button>
      {toast ? <div className="home-toast">{toast}</div> : null}
    </div>
  );
}

function allSearchableTools(): SearchableTool[] {
  return [...imageTools, ...fileTools, ...moreTools];
}

function searchTools(keyword: string) {
  const normalizedKeyword = keyword.trim().toLowerCase();
  const tools = allSearchableTools();
  if (!normalizedKeyword) {
    return tools.slice(0, 5);
  }
  return tools.filter((item) => item.label.toLowerCase().includes(normalizedKeyword));
}

function toolCategoryLabel(item: SearchableTool) {
  if (imageTools.some((tool) => tool.label === item.label)) {
    return '图片处理';
  }
  if (fileTools.some((tool) => tool.label === item.label)) {
    return '文件转换';
  }
  return '生活工具';
}

function ToolsScreen({ onOpenCompress, onOpenMore }: { onOpenCompress: () => void; onOpenMore: () => void }) {
  return (
    <div className="tool-screen">
      <header className="tool-page-head">
        <h1>工具中心</h1>
        <Search size={20} />
      </header>
      <SegmentedTabs items={['全部', '图片处理', '文件转换', '扫描识别', '生活工具']} />
      <ToolSection title="图片处理" tools={imageTools} onOpenCompress={onOpenCompress} />
      <ToolSection title="文件转换" tools={fileTools} />
      <ToolSection title="扫描识别" tools={imageTools.slice(2, 6)} />
      <button className="more-tools-entry" type="button" onClick={onOpenMore}>
        <span>
          <strong>更多工具</strong>
          <small>计算器、汇率、日期、密码生成等实用工具</small>
        </span>
        <ChevronRight size={18} />
      </button>
    </div>
  );
}

function ToolSection({ title, tools, onOpenCompress }: { title: string; tools: typeof imageTools; onOpenCompress?: () => void }) {
  return (
    <section className="tool-section">
      <h2>{title}</h2>
      <div className="tool-catalog-grid">
        {tools.map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" onClick={item.screen ? onOpenCompress : undefined}>
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={23} />
              </span>
              <em>{item.label}</em>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function CompressScreen({ onBack }: { onBack: () => void }) {
  return (
    <div className="tool-screen compress-screen">
      <HeaderWithBack title="图片压缩" onBack={onBack} right="使用帮助" />
      <section className="upload-card">
        <span className="upload-art">
          <Image size={48} />
          <i>+</i>
        </span>
        <strong>选择图片</strong>
        <p>支持 JPG、PNG、HEIC 等格式</p>
        <button className="app-primary" type="button">选择图片</button>
      </section>
      <section className="compress-settings">
        <h2>压缩设置</h2>
        <div className="mode-pills">
          <button className="active" type="button">智能压缩<br /><small>推荐</small></button>
          <button type="button">清晰优先</button>
          <button type="button">体积优先</button>
          <button type="button">自定义</button>
        </div>
        <label>
          <span>压缩质量</span>
          <strong>80%</strong>
        </label>
        <input className="quality-slider" type="range" min="0" max="100" defaultValue="80" />
        <div className="quality-labels">
          <span>体积更小</span>
          <span>均衡</span>
          <span>质量更高</span>
        </div>
      </section>
      <button className="app-primary fixed-action" type="button">开始压缩</button>
    </div>
  );
}

function ActivityScreen() {
  return (
    <div className="tool-screen">
      <header className="tool-page-head">
        <h1>活动中心</h1>
        <Bell size={20} />
      </header>
      <section className="vip-banner">
        <div>
          <h2>开通会员<br />解锁更多高级工具</h2>
          <p>更高效率 · 无限次数 · 专属功能</p>
          <button type="button">立即开通</button>
        </div>
        <Crown size={78} />
      </section>
      <SegmentedTabs items={['全部', '会员活动', '限时福利', '新功能']} />
      <div className="activity-list">
        {activityCards.map((item) => (
          <article key={item.title} className={`activity-card ${item.tone}`}>
            <div>
              <em>{item.tag}</em>
              <strong>{item.title}</strong>
              <span>{item.desc}</span>
            </div>
            <button type="button">{item.action}</button>
          </article>
        ))}
      </div>
    </div>
  );
}

function MessageScreen() {
  return (
    <div className="tool-screen">
      <header className="tool-page-head">
        <h1>消息</h1>
        <Settings size={20} />
      </header>
      <SegmentedTabs items={['全部', '系统通知', '活动消息', '功能更新']} />
      <div className="message-list">
        {messages.map((item) => {
          const Icon = item.icon;
          return (
            <article key={item.title} className="message-item">
              <span className={`tool-icon ${item.tone}`}>
                <Icon size={20} />
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.desc}</p>
              </div>
              <time>{item.time}</time>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function ProfileScreen({
  displayName,
  serviceStatus,
  loading,
  dynamic,
  onReload,
  onLogout,
  onOpenMembership,
  onOpenHistory,
  onOpenFavorites,
  onOpenTrips,
  onOpenSettings,
}: {
  displayName: string;
  serviceStatus: string;
  loading: boolean;
  dynamic: ProfileDynamic | null;
  onReload: () => void;
  onLogout: () => void;
  onOpenMembership: () => void;
  onOpenHistory: () => void;
  onOpenFavorites: () => void;
  onOpenTrips: () => void;
  onOpenSettings: () => void;
}) {
  const [statsMessage, setStatsMessage] = useState<string | null>(null);

  function showStatsMessage(message: string) {
    setStatsMessage(message);
    window.setTimeout(() => setStatsMessage(null), 1600);
  }

  return (
    <div className="tool-screen profile-screen">
      <header className="profile-head">
        <div className="profile-avatar">
          <UserRound size={34} />
        </div>
        <div>
          <h1>Hello, {displayName}</h1>
          <p>ID: 12345678 · {serviceStatus}</p>
        </div>
        <button type="button" aria-label="通知">
          <Bell size={22} />
          <i />
        </button>
      </header>
      <section className="profile-vip-card">
        <Crown size={24} />
        <div>
          <strong>开通会员</strong>
          <span>解锁更多高级功能</span>
        </div>
        <button type="button" onClick={onOpenMembership}>立即开通</button>
      </section>
      <div className="profile-stats">
        <button type="button" onClick={onOpenFavorites} aria-label="打开我的收藏">
          <strong>{dynamic?.favoriteCount ?? '-'}</strong>
          <span>我的收藏</span>
        </button>
        <button type="button" onClick={onOpenHistory} aria-label="打开历史记录">
          <strong>{dynamic?.historyCount ?? '-'}</strong>
          <span>历史记录</span>
        </button>
        <button type="button" onClick={() => showStatsMessage('我的文件功能建设中')} aria-label="打开我的文件">
          <strong>{dynamic?.fileCount ?? '-'}</strong>
          <span>我的文件</span>
        </button>
        <button type="button" onClick={() => showStatsMessage('优惠券功能建设中')} aria-label="打开优惠券">
          <strong>{dynamic?.couponCount ?? '-'}</strong>
          <span>优惠券</span>
        </button>
      </div>
      {statsMessage ? <div className="profile-stats-toast">{statsMessage}</div> : null}
      <div className="profile-menu">
        <MenuRow icon={Crown} title="会员中心" onClick={onOpenMembership} />
        <MenuRow icon={CalendarDays} title="我的行程" onClick={onOpenTrips} />
        <MenuRow icon={FileText} title="历史记录" onClick={onOpenHistory} />
        <MenuRow icon={Star} title="我的收藏" onClick={onOpenFavorites} />
        <MenuRow icon={RefreshCw} title={loading ? '刷新中' : '刷新动态'} onClick={onReload} />
        <MenuRow icon={Settings} title="设置" onClick={onOpenSettings} />
        <MenuRow icon={LogOut} title="退出登录" onClick={onLogout} />
      </div>
    </div>
  );
}

function HistoryScreen({ onBack }: { onBack: () => void }) {
  const [items, setItems] = useState<UserHistoryItem[]>([]);
  const [category, setCategory] = useState('全部');
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const categories = ['全部', '图片处理', '文件转换', '扫描识别', '生活工具'];

  useEffect(() => {
    void reload(category);
  }, [category]);

  async function reload(nextCategory: string) {
    setLoading(true);
    setMessage(null);
    try {
      const page = await loadHistory(nextCategory);
      setItems(page.records || []);
    } catch (err) {
      setMessage(readSettingError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="tool-screen">
      <HeaderWithBack title="历史记录" onBack={onBack} />
      <SegmentedTabs items={categories} active={category} onChange={setCategory} />
      <h2 className="history-date">今天</h2>
      {message ? <div className="settings-message error">{message}</div> : null}
      <div className="message-list history-list">
        {loading ? <article className="settings-info-card">加载中</article> : null}
        {!loading && items.length === 0 ? <article className="settings-info-card">暂无历史记录</article> : null}
        {items.map((item) => {
          const Icon = iconForTone(item.iconTone);
          return (
            <article key={item.id} className="message-item">
              <span className={`tool-icon ${item.iconTone || 'blue'}`}>
                <Icon size={20} />
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.description}</p>
              </div>
              <time>{formatShortTime(item.occurredAt)}</time>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function MembershipScreen({ onBack }: { onBack: () => void }) {
  const [membership, setMembership] = useState<Membership | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    loadMembership()
      .then(setMembership)
      .catch((err) => setMessage(readSettingError(err)));
  }, []);

  return (
    <div className="tool-screen">
      <HeaderWithBack title="会员中心" onBack={onBack} />
      {message ? <div className="settings-message error">{message}</div> : null}
      <section className="vip-banner">
        <div>
          <h2>{membership?.planName || '普通会员'}</h2>
          <p>{membership?.benefits || '基础工具可用'}</p>
          <button type="button">立即开通</button>
        </div>
        <Crown size={78} />
      </section>
      <div className="settings-card-list">
        <article className="settings-info-card">
          <strong>会员状态</strong>
          <span>{membership?.status || '-'}</span>
        </article>
        <article className="settings-info-card">
          <strong>到期时间</strong>
          <span>{membership?.expireAt ? formatShortTime(membership.expireAt) : '长期有效'}</span>
        </article>
      </div>
    </div>
  );
}

function FavoritesScreen({ onBack }: { onBack: () => void }) {
  const [items, setItems] = useState<FavoriteItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    loadFavorites()
      .then((page) => setItems(page.records || []))
      .catch((err) => setMessage(readSettingError(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="tool-screen">
      <HeaderWithBack title="我的收藏" onBack={onBack} />
      {message ? <div className="settings-message error">{message}</div> : null}
      <div className="message-list">
        {loading ? <article className="settings-info-card">加载中</article> : null}
        {!loading && items.length === 0 ? <article className="settings-info-card">暂无收藏</article> : null}
        {items.map((item) => {
          const Icon = iconForTone(item.iconTone);
          return (
            <article key={item.id} className="message-item">
              <span className={`tool-icon ${item.iconTone || 'blue'}`}>
                <Icon size={20} />
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.description}</p>
              </div>
              <time>{formatShortTime(item.createdAt)}</time>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function TripScheduleScreen({
  mode,
  displayName,
  username,
  onBack,
  onOpenMyTrips,
  onOpenBooking,
}: {
  mode: 'booking' | 'mine';
  displayName: string;
  username: string;
  onBack: () => void;
  onOpenMyTrips?: () => void;
  onOpenBooking?: () => void;
}) {
  const [tab, setTab] = useState(mode === 'mine' ? '我的行程' : '可预约');
  const [selectedTripOwner, setSelectedTripOwner] = useState<string | null>(null);
  const [owners, setOwners] = useState<TripOwner[]>([]);
  const [selectedOwnerSlots, setSelectedOwnerSlots] = useState<TripSlot[]>([]);
  const [mySlots, setMySlots] = useState<TripSlot[]>([]);
  const [reservedSlots, setReservedSlots] = useState<TripSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [title, setTitle] = useState('下午可约');
  const [place, setPlace] = useState('线上会议');
  const [date, setDate] = useState(nextTripDate(1));
  const [startTime, setStartTime] = useState('14:00');
  const [endTime, setEndTime] = useState('15:00');
  const [toast, setToast] = useState<string | null>(null);
  const pendingRequests = mySlots.filter((slot) => slot.status === 'PENDING');
  const startTimeOptions = buildTripStartTimeOptions(date);
  const endTimeOptions = buildTripEndTimeOptions(startTime);

  useEffect(() => {
    void reloadTrips();
  }, [mode]);

  async function reloadTrips(ownerUsername = selectedTripOwner) {
    setLoading(true);
    try {
      if (mode === 'booking') {
        const nextOwners = await loadTripOwners();
        const nextReservations = await loadMyTripReservations();
        const nextMySlots = await loadMyTrips();
        setOwners(nextOwners);
        setReservedSlots(nextReservations);
        setMySlots(nextMySlots);
        if (ownerUsername) {
          setSelectedOwnerSlots(await loadBookableTripSlots(ownerUsername));
        }
      } else {
        const [nextMySlots, nextReservations] = await Promise.all([
          loadMyTrips(),
          loadMyTripReservations(),
        ]);
        setMySlots(nextMySlots);
        setReservedSlots(nextReservations);
      }
    } catch (err) {
      showTripToast(readSettingError(err));
    } finally {
      setLoading(false);
    }
  }

  async function selectTripOwner(ownerUsername: string) {
    setSelectedTripOwner(ownerUsername);
    setLoading(true);
    try {
      setSelectedOwnerSlots(await loadBookableTripSlots(ownerUsername));
    } catch (err) {
      showTripToast(readSettingError(err));
    } finally {
      setLoading(false);
    }
  }

  async function publishSlot(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !place.trim() || !date || !startTime || !endTime) {
      showTripToast('请完整填写行程信息');
      return;
    }
    if (startTime >= endTime) {
      showTripToast('结束时间需晚于开始时间');
      return;
    }
    const request: TripPublishRequest = {
      title: title.trim(),
      place: place.trim(),
      tripDate: date,
      startTime,
      endTime,
    };
    try {
      await publishTrip(request);
      await reloadTrips();
      setTab('我的发布');
      setTitle('下午可约');
      setPlace('线上会议');
      showTripToast('行程已发布');
    } catch (err) {
      showTripToast(readSettingError(err));
    }
  }

  function changeTripDate(nextDate: string) {
    const nextStartOptions = buildTripStartTimeOptions(nextDate);
    const nextStartTime = nextStartOptions.includes(startTime) ? startTime : nextStartOptions[0] || startTime;
    const nextEndOptions = buildTripEndTimeOptions(nextStartTime);
    setDate(nextDate);
    setStartTime(nextStartTime);
    if (!nextEndOptions.includes(endTime)) {
      setEndTime(nextEndOptions[0] || endTime);
    }
  }

  function changeStartTime(nextStartTime: string) {
    const nextEndOptions = buildTripEndTimeOptions(nextStartTime);
    setStartTime(nextStartTime);
    if (!nextEndOptions.includes(endTime)) {
      setEndTime(nextEndOptions[0] || endTime);
    }
  }

  async function requestSlot(slot: TripSlot) {
    if (isOwnTripSlot(slot, username)) {
      showTripToast('不能预约自己发布的行程');
      return;
    }
    try {
      await applyTrip(slot.id);
      await reloadTrips();
      setTab('我的预约');
      showTripToast('预约申请已发送');
    } catch (err) {
      if (err instanceof ApiError && err.code === TRIP_CONFLICT_CODE) {
        const confirmed = window.confirm('预约时间与您发布的行程冲突。继续预约将删除自己发布的冲突行程，是否继续？');
        if (!confirmed) {
          return;
        }
        try {
          await applyTrip(slot.id, '希望预约这个时段', true);
          await reloadTrips();
          setTab('我的预约');
          showTripToast('已删除冲突行程并提交预约');
        } catch (confirmErr) {
          showTripToast(readSettingError(confirmErr));
        }
        return;
      }
      showTripToast(readSettingError(err));
    }
  }

  function reviewSlot(slotId: number, approved: boolean) {
    reviewTrip(slotId, approved)
      .then(() => reloadTrips())
      .then(() => showTripToast(approved ? '已通过预约申请' : '已拒绝预约申请'))
      .catch((err) => showTripToast(readSettingError(err)));
  }

  function changeTripTab(nextTab: string) {
    setTab(nextTab);
    if (mode !== 'booking' || nextTab !== '可预约') {
      setSelectedTripOwner(null);
      setSelectedOwnerSlots([]);
    }
    void reloadTrips();
  }

  function showTripToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(null), 1600);
  }

  return (
    <div className="tool-screen trip-screen">
      <HeaderWithBack title={mode === 'mine' ? '我的行程' : '行程预约'} onBack={onBack} right={mode === 'mine' ? '管理' : '预约'} />
      <section className="trip-hero">
        <div>
          <span>最近一周</span>
          <h2>{mode === 'mine' ? '查看我的行程、预约和审核' : '先选择可预约人，再查看我的预约'}</h2>
        </div>
        <CalendarDays size={58} />
      </section>

      <div className="trip-segment-row">
        {mode === 'mine' ? (
          <button className="trip-flow-jump backward" type="button" onClick={onOpenBooking} aria-label="返回行程预约">
            <ChevronLeft size={17} />
          </button>
        ) : null}
        <SegmentedTabs items={mode === 'mine' ? ['我的行程', '我的预约', '我的发布', '待审核'] : ['可预约', '我的预约', '行程预览']} active={tab} onChange={changeTripTab} />
        {mode === 'booking' ? (
          <button className="trip-flow-jump forward" type="button" onClick={onOpenMyTrips} aria-label="打开我的行程">
            <ChevronRight size={17} />
          </button>
        ) : null}
      </div>

      {tab === '我的行程' ? (
        <TripSlotList emptyText={loading ? '加载中' : '暂无我的行程'} slots={mySlots} currentUser={username} />
      ) : null}

      {tab === '我的发布' ? (
        <>
          <form className="trip-form" onSubmit={publishSlot}>
            <label>
              <span>行程标题</span>
              <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="例如 咖啡沟通 / 线上咨询" />
            </label>
            <label>
              <span>地点</span>
              <input value={place} onChange={(event) => setPlace(event.target.value)} placeholder="例如 公司会议室 / 线上会议" />
            </label>
            <div className="trip-time-grid">
              <label>
                <span>日期</span>
                <input type="date" min={nextTripDate(0)} max={nextTripDate(6)} value={date} onChange={(event) => changeTripDate(event.target.value)} />
              </label>
              <label>
                <span>开始</span>
                <TripTimePicker value={startTime} options={startTimeOptions} onChange={changeStartTime} hourMode={date === nextTripDate(0) ? 'options' : 'all'} />
              </label>
              <label>
                <span>结束</span>
                <TripTimePicker value={endTime} options={endTimeOptions} onChange={setEndTime} hourMode="options" />
              </label>
            </div>
            <button className="app-primary" type="submit">发布行程</button>
          </form>
          <TripSlotList emptyText={loading ? '加载中' : '暂无已发布行程'} slots={mySlots} currentUser={username} />
        </>
      ) : null}

      {tab === '我的预约' ? (
        <TripSlotList emptyText={loading ? '加载中' : '暂无我的预约'} slots={reservedSlots} currentUser={username} statusContext="reservation" />
      ) : null}

      {tab === '行程预览' ? (
        <TripTimelinePreview slots={mergeTripTimelineSlots(mySlots, reservedSlots)} currentUser={username} />
      ) : null}

      {mode === 'booking' && tab === '可预约' ? (
        selectedTripOwner ? (
          <>
            <button className="trip-owner-back" type="button" onClick={() => setSelectedTripOwner(null)}>
              <ChevronLeft size={17} />
              返回可预约人列表
            </button>
            <TripSlotList
              emptyText="该用户暂无可预约时段"
              slots={selectedOwnerSlots}
              currentUser={username}
              actionLabel="预约"
              onAction={requestSlot}
            />
          </>
        ) : (
          <TripOwnerList owners={owners} loading={loading} onSelect={selectTripOwner} />
        )
      ) : null}

      {tab === '待审核' ? (
        <div className="trip-list">
          {pendingRequests.length === 0 ? <article className="settings-info-card">暂无待审核申请</article> : null}
          {pendingRequests.map((slot) => (
            <article key={slot.id} className="trip-card">
              <TripCardMain slot={slot} currentUser={username} />
              <div className="trip-request-note">
                <UserCheck size={16} />
                <span>{slot.applicantDisplayName || slot.applicantUsername} 申请预约：{slot.applyNote}</span>
              </div>
              <div className="trip-review-actions">
                <button type="button" onClick={() => reviewSlot(slot.id, false)}>拒绝</button>
                <button type="button" onClick={() => reviewSlot(slot.id, true)}>
                  <CheckCircle2 size={16} />
                  通过
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {toast ? <div className="home-toast">{toast}</div> : null}
    </div>
  );
}

function TripOwnerList({ owners, loading, onSelect }: { owners: TripOwner[]; loading: boolean; onSelect: (ownerName: string) => void }) {
  return (
    <div className="trip-owner-list">
      {owners.length === 0 ? <article className="settings-info-card">{loading ? '加载中' : '暂无可预约用户'}</article> : null}
      {owners.map((owner) => (
        <button key={owner.ownerUsername} className="trip-owner-card" type="button" onClick={() => onSelect(owner.ownerUsername)}>
          <span className="trip-owner-avatar">
            <UserRound size={20} />
          </span>
          <span>
            <strong>{owner.ownerDisplayName || owner.ownerUsername}</strong>
            <small>{owner.slotCount} 个可预约时段 · 最近 {formatTripDate(owner.nextDate)} {owner.nextTime}</small>
          </span>
          <ChevronRight size={17} />
        </button>
      ))}
    </div>
  );
}

function TripTimePicker({
  value,
  options,
  onChange,
  hourMode = 'all',
}: {
  value: string;
  options: string[];
  onChange: (value: string) => void;
  hourMode?: 'all' | 'options';
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(value);
  const selectedHour = value.slice(0, 2);
  const hours = hourMode === 'all' ? buildTripHourOptions() : Array.from(new Set(options.map((time) => time.slice(0, 2))));
  const activeHour = hours.includes(selectedHour) ? selectedHour : hours[0] || selectedHour;
  const minutes = options
    .filter((time) => time.startsWith(`${activeHour}:`))
    .map((time) => time.slice(3, 5));

  useEffect(() => {
    setDraft(value);
  }, [value]);

  function closeWhenLeaving(event: FocusEvent<HTMLDivElement>) {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setOpen(false);
      if (!isAllowedTripTime(draft, options)) {
        setDraft(value);
      }
    }
  }

  function applyManualInput(nextDraft: string) {
    setDraft(nextDraft);
    const normalized = normalizeTripTimeInput(nextDraft);
    if (normalized && options.includes(normalized)) {
      onChange(normalized);
    }
  }

  function chooseHour(hour: string) {
    const nextTime = options.find((time) => time.startsWith(`${hour}:`));
    if (nextTime) {
      onChange(nextTime);
    }
  }

  function chooseMinute(minute: string) {
    const nextTime = `${activeHour}:${minute}`;
    if (options.includes(nextTime)) {
      onChange(nextTime);
    }
  }

  return (
    <div className="trip-time-picker" onBlur={closeWhenLeaving}>
      <div className="trip-time-input-wrap">
        <input
          value={draft}
          inputMode="numeric"
          placeholder="HH:mm"
          onFocus={() => setOpen(true)}
          onChange={(event) => applyManualInput(event.target.value)}
        />
        <button type="button" aria-label="选择时间" onClick={() => setOpen((next) => !next)}>
          <ChevronRight size={15} />
        </button>
      </div>
      {open ? (
        <div className="trip-time-panel">
          <div className="trip-time-column" aria-label="小时">
            {hours.map((hour) => (
              <button key={hour} className={hour === activeHour ? 'active' : ''} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => chooseHour(hour)}>
                {hour}
              </button>
            ))}
          </div>
          <div className="trip-time-column" aria-label="分钟">
            {minutes.map((minute) => (
              <button key={minute} className={`${activeHour}:${minute}` === value ? 'active' : ''} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => chooseMinute(minute)}>
                {minute}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function TripTimelinePreview({ slots, currentUser }: { slots: TripSlot[]; currentUser: string }) {
  const now = new Date();
  const sortedSlots = [...slots].sort(compareTripSlots);
  const upcomingSlots = sortedSlots.filter((slot) => !isTripSlotExpired(slot, now));
  const expiredSlots = sortedSlots.filter((slot) => isTripSlotExpired(slot, now)).reverse();

  return (
    <section className="trip-timeline-preview">
      <header>
        <span>行程预览</span>
        <strong>{upcomingSlots.length} 个未出行 · {expiredSlots.length} 个已过期</strong>
      </header>
      <TripTimelineGroup
        title="未出行"
        description="今天未结束和未来行程"
        emptyText="暂无未出行行程"
        slots={upcomingSlots}
        currentUser={currentUser}
      />
      <TripTimelineGroup
        title="已过期"
        description="已超过结束时间的行程"
        emptyText="暂无已过期行程"
        slots={expiredSlots}
        currentUser={currentUser}
        expired
      />
    </section>
  );
}

function TripTimelineGroup({
  title,
  description,
  emptyText,
  slots,
  currentUser,
  expired = false,
}: {
  title: string;
  description: string;
  emptyText: string;
  slots: TripSlot[];
  currentUser: string;
  expired?: boolean;
}) {
  return (
    <div className="trip-timeline-group">
      <div className="trip-timeline-group-title">
        <strong>{title}</strong>
        <span>{description}</span>
      </div>
      {slots.length === 0 ? <article className="settings-info-card">{emptyText}</article> : null}
      {slots.map((slot) => {
        const ownSlot = isOwnTripSlot(slot, currentUser);
        return (
          <article key={`${slot.id}-${ownSlot ? 'owned' : 'reserved'}-${expired ? 'expired' : 'upcoming'}`} className={`trip-timeline-item ${ownSlot ? 'owned' : 'reserved'} ${expired ? 'expired' : 'upcoming'}`}>
            <time>
              <span>{formatTripDate(slot.tripDate)}</span>
              {formatTripTimeRange(slot.startTime, slot.endTime)}
            </time>
            <div>
              <strong>{slot.title}</strong>
              <span>{ownSlot ? tripSubtitle(slot, true) : tripSubtitle(slot, false, 'reservation')}</span>
              <small>{slot.place} · {tripStatusLabel(slot.status, ownSlot ? undefined : 'reservation')} · {expired ? '已过期' : '未出行'}</small>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function TripSlotList({
  slots,
  currentUser,
  emptyText,
  actionLabel,
  onAction,
  statusContext,
}: {
  slots: TripSlot[];
  currentUser: string;
  emptyText: string;
  actionLabel?: string;
  onAction?: (slot: TripSlot) => void;
  statusContext?: 'reservation';
}) {
  return (
    <div className="trip-list">
      {slots.length === 0 ? <article className="settings-info-card">{emptyText}</article> : null}
      {slots.map((slot) => (
        <article key={slot.id} className="trip-card">
          <TripCardMain slot={slot} currentUser={currentUser} statusContext={statusContext} />
          {actionLabel && onAction ? (
            <button className="trip-card-action" type="button" onClick={() => onAction(slot)}>{actionLabel}</button>
          ) : null}
        </article>
      ))}
    </div>
  );
}

function mergeTripTimelineSlots(mySlots: TripSlot[], reservedSlots: TripSlot[]) {
  const byKey = new Map<string, TripSlot>();
  [...mySlots, ...reservedSlots].forEach((slot) => {
    byKey.set(`${slot.id}-${slot.ownerUsername}-${slot.applicantUsername || ''}`, slot);
  });
  return Array.from(byKey.values());
}

function compareTripSlots(left: TripSlot, right: TripSlot) {
  return `${left.tripDate} ${left.startTime}`.localeCompare(`${right.tripDate} ${right.startTime}`);
}

function isTripSlotExpired(slot: TripSlot, now = new Date()) {
  const endAt = new Date(`${slot.tripDate}T${formatTripClock(slot.endTime)}:00`);
  return !Number.isNaN(endAt.getTime()) && endAt.getTime() < now.getTime();
}

function TripCardMain({ slot, currentUser, statusContext }: { slot: TripSlot; currentUser: string; statusContext?: 'reservation' }) {
  const ownSlot = isOwnTripSlot(slot, currentUser);

  return (
    <div className="trip-card-main">
      <span
        className={`trip-status ${slot.status}`}
        title={tripStatusHint(slot.status, statusContext)}
        tabIndex={statusContext === 'reservation' && slot.status === 'PENDING' ? 0 : undefined}
      >
        {tripStatusLabel(slot.status, statusContext)}
      </span>
      <div>
        <strong>{slot.title}</strong>
        <p>{tripSubtitle(slot, ownSlot, statusContext)}</p>
      </div>
      <div className="trip-meta">
        <span><CalendarDays size={15} />{formatTripDate(slot.tripDate)}</span>
        <span><Clock3 size={15} />{slot.startTime}-{slot.endTime}</span>
        <span>{slot.place}</span>
      </div>
    </div>
  );
}

function tripSubtitle(slot: TripSlot, ownSlot: boolean, context?: 'reservation') {
  if (context === 'reservation') {
    const owner = slot.ownerDisplayName || slot.ownerUsername;
    return owner ? `预约对象：${owner}` : '预约对象：-';
  }
  if (!ownSlot) {
    return `${slot.ownerDisplayName || slot.ownerUsername} 发布的行程`;
  }
  const applicant = slot.applicantDisplayName || slot.applicantUsername;
  return applicant ? `我发布的行程 · 预约人：${applicant}` : '我发布的行程';
}

function SettingsScreen({ displayName, onBack, onLogout }: { displayName: string; onBack: () => void; onLogout: () => void }) {
  const [detail, setDetail] = useState<SettingDetail | null>(null);
  const [cacheSize, setCacheSize] = useState('12.4MB');
  const [toast, setToast] = useState<string | null>(null);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(null), 1800);
  }

  function clearCache() {
    if (cacheSize === '0KB') {
      showToast('缓存已清理');
      return;
    }
    setCacheSize('0KB');
    showToast('缓存清理完成');
  }

  function checkVersion() {
    setDetail('version');
  }

  function logout() {
    if (window.confirm('确认退出登录？')) {
      onLogout();
    }
  }

  if (detail) {
    return (
      <div className="tool-screen settings-detail-screen">
        <HeaderWithBack title={settingTitle(detail)} onBack={() => setDetail(null)} />
        {detail === 'account' ? (
          <AccountSecurityDetail fallbackDisplayName={displayName} />
        ) : (
          <SettingDetailContent
            detail={detail}
          />
        )}
      </div>
    );
  }

  return (
    <div className="tool-screen">
      <HeaderWithBack title="设置" onBack={onBack} />
      <div className="profile-menu settings-menu">
        <MenuRow icon={ShieldCheck} title="账号与安全" onClick={() => setDetail('account')} />
        <MenuRow icon={Bell} title="消息通知" onClick={() => setDetail('notifications')} />
        <MenuRow icon={Archive} title="清理缓存" meta={cacheSize} onClick={clearCache} />
        <MenuRow icon={FileText} title="关于我们" onClick={() => setDetail('about')} />
        <MenuRow icon={ShieldCheck} title="隐私政策" onClick={() => setDetail('privacy')} />
        <MenuRow icon={FileText} title="用户协议" onClick={() => setDetail('agreement')} />
        <MenuRow icon={RefreshCw} title="版本更新" meta="v1.2.0" onClick={checkVersion} />
      </div>
      <button className="logout-button" type="button" onClick={logout}>退出登录</button>
      {toast ? <div className="settings-toast">{toast}</div> : null}
    </div>
  );
}

function AccountSecurityDetail({ fallbackDisplayName }: { fallbackDisplayName: string }) {
  const [account, setAccount] = useState<AccountSecurity | null>(null);
  const [displayName, setDisplayName] = useState(fallbackDisplayName);
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    void reload();
  }, []);

  async function reload() {
    setLoading(true);
    setMessage(null);
    try {
      const nextAccount = await loadAccountSecurity();
      setAccount(nextAccount);
      setDisplayName(nextAccount.displayName || '');
      setEmail(nextAccount.email || '');
      setPhone(nextAccount.phone || '');
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setLoading(false);
    }
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!displayName.trim()) {
      setMessage({ type: 'error', text: '请输入显示名称' });
      return;
    }
    if (email.trim() && !isValidEmail(email.trim())) {
      setMessage({ type: 'error', text: '请输入有效的邮箱地址' });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const nextAccount = await updateAccountSecurity({
        displayName: displayName.trim(),
        email: email.trim() || null,
        phone: phone.trim() || null,
      });
      setAccount(nextAccount);
      setDisplayName(nextAccount.displayName || '');
      setEmail(nextAccount.email || '');
      setPhone(nextAccount.phone || '');
      setMessage({ type: 'success', text: '账号资料已更新' });
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setSaving(false);
    }
  }

  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentPassword || !newPassword) {
      setMessage({ type: 'error', text: '请输入当前密码和新密码' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setMessage({ type: 'error', text: '两次输入的新密码不一致' });
      return;
    }
    if (!isStrongPassword(newPassword)) {
      setMessage({ type: 'error', text: '新密码需为 6-20 位，并包含数字、大小写字母和特殊字符' });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      await changeAccountPassword({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setMessage({ type: 'success', text: '密码已修改' });
    } catch (err) {
      setMessage({ type: 'error', text: readSettingError(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="settings-card-list">
      <article className="settings-info-card">
        <strong>{account?.username || '-'}</strong>
        <span>{loading ? '账号信息加载中' : `会员等级：${account?.vipLevel || '-'} · 状态：${account?.status || '-'}`}</span>
      </article>

      {message ? <div className={`settings-message ${message.type}`}>{message.text}</div> : null}

      <form className="settings-form-card" onSubmit={saveProfile}>
        <h2>基础信息</h2>
        <label>
          显示名称
          <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} disabled={loading || saving} />
        </label>
        <label>
          邮箱
          <input value={email} onChange={(event) => setEmail(event.target.value)} disabled={loading || saving} inputMode="email" />
        </label>
        <label>
          手机号
          <input value={phone} onChange={(event) => setPhone(event.target.value)} disabled={loading || saving} inputMode="tel" />
        </label>
        <button className="app-primary" type="submit" disabled={loading || saving}>
          {saving ? '保存中' : '保存资料'}
        </button>
      </form>

      <form className="settings-form-card" onSubmit={savePassword}>
        <h2>修改密码</h2>
        <label>
          当前密码
          <input type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} disabled={saving} autoComplete="current-password" />
        </label>
        <label>
          新密码
          <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} disabled={saving} autoComplete="new-password" />
        </label>
        <label>
          确认新密码
          <input type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} disabled={saving} autoComplete="new-password" />
        </label>
        <button className="app-primary" type="submit" disabled={saving}>
          {saving ? '提交中' : '修改密码'}
        </button>
      </form>
    </div>
  );
}

function settingTitle(detail: SettingDetail) {
  return {
    account: '账号与安全',
    notifications: '消息通知',
    about: '关于我们',
    privacy: '隐私政策',
    agreement: '用户协议',
    version: '版本更新',
  }[detail];
}

function SettingDetailContent({
  detail,
}: {
  detail: SettingDetail;
}) {
  const [noticeSettings, setNoticeSettings] = useState({
    systemEnabled: true,
    activityEnabled: true,
    taskEnabled: false,
  });
  const [document, setDocument] = useState<AppDocument | null>(null);
  const [version, setVersion] = useState('检查中');
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    setMessage(null);
    if (detail === 'notifications') {
      loadNotificationSettings()
        .then(setNoticeSettings)
        .catch((err) => setMessage(readSettingError(err)));
      return;
    }
    if (detail === 'version') {
      loadAppVersion()
        .then((nextVersion) => setVersion(nextVersion.latest ? '已是最新' : nextVersion.versionName))
        .catch((err) => setMessage(readSettingError(err)));
      return;
    }
    if (detail === 'about' || detail === 'privacy' || detail === 'agreement') {
      loadAppDocument(detail)
        .then(setDocument)
        .catch((err) => setMessage(readSettingError(err)));
    }
  }, [detail]);

  async function saveNotice(key: keyof typeof noticeSettings, value: boolean) {
    const next = { ...noticeSettings, [key]: value };
    setNoticeSettings(next);
    try {
      setNoticeSettings(await updateNotificationSettings(next));
    } catch (err) {
      setMessage(readSettingError(err));
    }
  }

  if (detail === 'notifications') {
    return (
      <div className="settings-card-list">
        {message ? <div className="settings-message error">{message}</div> : null}
        <SwitchRow title="系统通知" desc="登录状态、账号安全和服务状态提醒" checked={noticeSettings.systemEnabled} onChange={(value) => saveNotice('systemEnabled', value)} />
        <SwitchRow title="活动消息" desc="会员活动、福利和优惠通知" checked={noticeSettings.activityEnabled} onChange={(value) => saveNotice('activityEnabled', value)} />
        <SwitchRow title="任务完成提醒" desc="文件处理、图片压缩完成后提醒" checked={noticeSettings.taskEnabled} onChange={(value) => saveNotice('taskEnabled', value)} />
      </div>
    );
  }

  if (detail === 'version') {
    return (
      <div className="settings-card-list">
        {message ? <div className="settings-message error">{message}</div> : null}
        <article className="settings-info-card centered">
          <RefreshCw size={34} />
          <strong>{version}</strong>
          <span>{version === '已是最新' ? '当前已是最新版本' : '当前版本'}</span>
        </article>
      </div>
    );
  }

  if (detail === 'account') {
    return null;
  }

  return (
    <article className="settings-document">
      {message ? <div className="settings-message error">{message}</div> : null}
      <h2>{document?.title || '加载中'}</h2>
      {(document?.content ? document.content.split('。').filter(Boolean).map((item) => `${item}。`) : ['内容加载中']).map((paragraph) => (
        <p key={paragraph}>{paragraph}</p>
      ))}
    </article>
  );
}

function readSettingError(error: unknown) {
  if (error instanceof Error) {
    if (error.message === 'email already exists') {
      return '该邮箱已经被占用';
    }
    if (error.message === 'email format is invalid') {
      return '请输入有效的邮箱地址';
    }
    if (error.message === 'Current password is incorrect') {
      return '当前密码不正确';
    }
    if (error.message === 'newPassword is not strong enough') {
      return '新密码需为 6-20 位，并包含数字、大小写字母和特殊字符';
    }
    if (error.message === 'trip slot time conflicts') {
      return '该时间段已有发布行程，请更换时间';
    }
    if (error.message === 'duplicate trip slot') {
      return '该时间段已有发布行程，请更换时间';
    }
    return error.message;
  }
  return '操作失败';
}

function isStrongPassword(value: string) {
  return /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{6,20}$/.test(value);
}

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value);
}

function iconForTone(tone?: string | null) {
  if (tone === 'green') {
    return Image;
  }
  if (tone === 'orange') {
    return FileText;
  }
  if (tone === 'red') {
    return FileImage;
  }
  if (tone === 'purple') {
    return Sparkles;
  }
  return FileText;
}

function formatShortTime(value?: string | null) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function nextTripDate(offsetDays: number) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
}

function buildTripTimeOptions(startMinute: number, endMinute: number) {
  const options: string[] = [];
  for (let minute = startMinute; minute <= endMinute; minute += 10) {
    options.push(formatTripTimeMinute(minute));
  }
  return options;
}

function buildTripHourOptions() {
  return Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, '0'));
}

function buildTripStartTimeOptions(date: string) {
  const today = nextTripDate(0);
  const minMinute = date === today ? Math.max(tripStartMinute, nextTenMinuteAfter(new Date())) : tripStartMinute;
  return buildTripTimeOptions(minMinute, tripEndMinute - 10);
}

function buildTripEndTimeOptions(startTime: string) {
  const startMinute = parseTripTimeMinute(startTime);
  const minMinute = startMinute + 10;
  const maxMinute = Math.min(tripEndMinute, startMinute + tripDurationLimitMinutes);
  return buildTripTimeOptions(minMinute, maxMinute);
}

function formatTripTimeMinute(totalMinutes: number) {
  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function formatTripTimeRange(startTime: string, endTime: string) {
  return `${formatTripClock(startTime)}-${formatTripClock(endTime)}`;
}

function formatTripClock(value: string) {
  return value.slice(0, 5);
}

function parseTripTimeMinute(value: string) {
  const [hour = '0', minute = '0'] = value.split(':');
  return Number(hour) * 60 + Number(minute);
}

function normalizeTripTimeInput(value: string) {
  const normalized = value.trim();
  const match = normalized.match(/^(\d{1,2}):?(\d{2})$/);
  if (!match) {
    return null;
  }
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return null;
  }
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function isAllowedTripTime(value: string, options: string[]) {
  const normalized = normalizeTripTimeInput(value);
  return Boolean(normalized && options.includes(normalized));
}

function nextTenMinuteAfter(date: Date) {
  const minutes = date.getHours() * 60 + date.getMinutes();
  return Math.floor(minutes / 10) * 10 + 10;
}

function formatTripDate(value: string) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return `${date.getMonth() + 1}/${date.getDate()} ${weekdays[date.getDay()]}`;
}

function tripStatusLabel(status: TripSlot['status'], context?: 'reservation') {
  const labels: Record<TripSlot['status'], string> = {
    OPEN: '可预约',
    PENDING: context === 'reservation' ? '预约中' : '待审核',
    BOOKED: '已预约',
  };
  return labels[status];
}

function tripStatusHint(status: TripSlot['status'], context?: 'reservation') {
  if (context === 'reservation' && status === 'PENDING') {
    return '等待对方确认';
  }
  return undefined;
}

function isOwnTripSlot(slot: TripSlot, displayName: string) {
  return normalizeTripOwner(slot.ownerUsername) === normalizeTripOwner(displayName);
}

function normalizeTripOwner(value: string) {
  return value.trim().toLowerCase();
}

function SwitchRow({ title, desc, checked, onChange }: { title: string; desc: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="settings-switch-row">
      <span>
        <strong>{title}</strong>
        <small>{desc}</small>
      </span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <i />
    </label>
  );
}

function MoreToolsSheet({ onClose }: { onClose: () => void }) {
  return (
    <div className="more-tools-mask">
      <section className="more-tools-sheet">
        <header>
          <h2>更多工具</h2>
          <button type="button" onClick={onClose} aria-label="关闭">
            <X size={20} />
          </button>
        </header>
        <div className="tool-catalog-grid">
          {moreTools.map((item) => {
            const Icon = item.icon;
            return (
              <button key={item.label} type="button">
                <span className={`tool-icon ${item.tone}`}>
                  <Icon size={22} />
                </span>
                <em>{item.label}</em>
              </button>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function BottomTabs({ active, onChange, hasMessageDot }: { active: TabKey; onChange: (tab: TabKey) => void; hasMessageDot?: boolean }) {
  const tabs = [
    { key: 'home' as const, icon: Home, label: '首页' },
    { key: 'tools' as const, icon: Grid2X2, label: '工具' },
    { key: 'activity' as const, icon: Star, label: '活动' },
    { key: 'message' as const, icon: MessageCircle, label: '消息', dot: hasMessageDot },
    { key: 'profile' as const, icon: UserRound, label: '我的' },
  ];

  return (
    <nav className="app-bottom-tabs" aria-label="底部导航">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        return (
          <button key={tab.key} className={active === tab.key ? 'active' : ''} type="button" onClick={() => onChange(tab.key)}>
            <span>
              <Icon size={21} />
              {tab.dot ? <i /> : null}
            </span>
            <em>{tab.label}</em>
          </button>
        );
      })}
    </nav>
  );
}

function HeaderWithBack({ title, onBack, right }: { title: string; onBack: () => void; right?: string }) {
  return (
    <header className="tool-detail-head">
      <button type="button" onClick={onBack} aria-label="返回">
        <ChevronLeft size={22} />
      </button>
      <h1>{title}</h1>
      <span>{right}</span>
    </header>
  );
}

function SectionTitle({ title, action }: { title: string; action?: string }) {
  return (
    <div className="tool-section-title">
      <h2>{title}</h2>
      {action ? <button type="button">{action}<ChevronRight size={14} /></button> : null}
    </div>
  );
}

function SegmentedTabs({ items, active, onChange }: { items: string[]; active?: string; onChange?: (item: string) => void }) {
  return (
    <div className="tool-segments">
      {items.map((item, index) => (
        <button key={item} className={(active ? active === item : index === 0) ? 'active' : ''} type="button" onClick={() => onChange?.(item)}>{item}</button>
      ))}
    </div>
  );
}

function MenuRow({ icon: Icon, title, meta, onClick }: { icon: typeof Settings; title: string; meta?: string; onClick?: () => void }) {
  return (
    <button type="button" onClick={onClick}>
      <Icon size={18} />
      <span>{title}</span>
      {meta ? <small>{meta}</small> : null}
      <ChevronRight size={17} />
    </button>
  );
}
