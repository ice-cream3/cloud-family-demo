import { useState } from 'react';
import {
  Archive,
  Bell,
  Calculator,
  Camera,
  ChevronLeft,
  ChevronRight,
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
  UserRound,
  Wand2,
  Wrench,
  X,
  Zap,
} from 'lucide-react';
import type { PartnerHealth, UserProfile } from '../types/api';
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
type ScreenKey = TabKey | 'compress' | 'history' | 'settings';

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

const historyItems = [
  { icon: Image, title: '图片压缩', desc: '3张图片 · 2.4MB -> 856KB', time: '14:32', tone: 'green' },
  { icon: FileImage, title: 'PDF转图片', desc: '文件名：document.pdf', time: '11:20', tone: 'red' },
  { icon: ScanLine, title: '文字识别', desc: '提取 365 个文字', time: '10:15', tone: 'blue' },
  { icon: FileText, title: '图片转PDF', desc: '5张图片', time: '昨天 16:20', tone: 'orange' },
  { icon: Calculator, title: '单位换算', desc: '长度单位转换', time: '昨天 09:45', tone: 'green' },
];

export function HomePage({ session, profile, health, loading, error, onReload, onLogout }: HomePageProps) {
  const [screen, setScreen] = useState<ScreenKey>('home');
  const [showMoreTools, setShowMoreTools] = useState(false);
  const displayName = profile?.displayName || session.username || '用户名';
  const serviceStatus = health?.status || (profile ? 'UP' : 'UNKNOWN');
  const activeTab = ['compress', 'history', 'settings'].includes(screen) ? 'tools' : screen as TabKey;

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
          <HomeScreen displayName={displayName} onOpenTools={() => setTab('tools')} onOpenCompress={() => setScreen('compress')} />
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
            loading={loading}
            onReload={onReload}
            onLogout={onLogout}
            onOpenHistory={() => setScreen('history')}
            onOpenSettings={() => setScreen('settings')}
          />
        ) : null}
        {screen === 'history' ? <HistoryScreen onBack={() => setScreen('profile')} /> : null}
        {screen === 'settings' ? <SettingsScreen onBack={() => setScreen('profile')} onLogout={onLogout} /> : null}

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

function HomeScreen({ displayName, onOpenTools, onOpenCompress }: { displayName: string; onOpenTools: () => void; onOpenCompress: () => void }) {
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
        <button type="button" aria-label="刷新">
          <Zap size={16} />
        </button>
      </header>

      <div className="tool-search">
        <input placeholder="搜索工具，试试“图片压缩”" />
        <Search size={18} />
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
    </div>
  );
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
  onReload,
  onLogout,
  onOpenHistory,
  onOpenSettings,
}: {
  displayName: string;
  serviceStatus: string;
  loading: boolean;
  onReload: () => void;
  onLogout: () => void;
  onOpenHistory: () => void;
  onOpenSettings: () => void;
}) {
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
        <button type="button">立即开通</button>
      </section>
      <div className="profile-stats">
        <div><strong>12</strong><span>我的收藏</span></div>
        <div><strong>28</strong><span>历史记录</span></div>
        <div><strong>3</strong><span>我的文件</span></div>
        <div><strong>0</strong><span>优惠券</span></div>
      </div>
      <div className="profile-menu">
        <MenuRow icon={Crown} title="会员中心" />
        <MenuRow icon={FileText} title="历史记录" onClick={onOpenHistory} />
        <MenuRow icon={Star} title="我的收藏" />
        <MenuRow icon={RefreshCw} title={loading ? '刷新中' : '刷新状态'} onClick={onReload} />
        <MenuRow icon={Settings} title="设置" onClick={onOpenSettings} />
        <MenuRow icon={LogOut} title="退出登录" onClick={onLogout} />
      </div>
    </div>
  );
}

function HistoryScreen({ onBack }: { onBack: () => void }) {
  return (
    <div className="tool-screen">
      <HeaderWithBack title="历史记录" onBack={onBack} />
      <SegmentedTabs items={['全部', '图片处理', '文件转换', '扫描识别', '生活工具']} />
      <h2 className="history-date">今天</h2>
      <div className="message-list history-list">
        {historyItems.map((item) => {
          const Icon = item.icon;
          return (
            <article key={`${item.title}-${item.time}`} className="message-item">
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

function SettingsScreen({ onBack, onLogout }: { onBack: () => void; onLogout: () => void }) {
  return (
    <div className="tool-screen">
      <HeaderWithBack title="设置" onBack={onBack} />
      <div className="profile-menu settings-menu">
        <MenuRow icon={ShieldCheck} title="账号与安全" />
        <MenuRow icon={Bell} title="消息通知" />
        <MenuRow icon={Archive} title="清理缓存" meta="12.4MB" />
        <MenuRow icon={FileText} title="关于我们" />
        <MenuRow icon={ShieldCheck} title="隐私政策" />
        <MenuRow icon={FileText} title="用户协议" />
        <MenuRow icon={RefreshCw} title="版本更新" meta="v1.2.0" />
      </div>
      <button className="logout-button" type="button" onClick={onLogout}>退出登录</button>
    </div>
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

function SegmentedTabs({ items }: { items: string[] }) {
  return (
    <div className="tool-segments">
      {items.map((item, index) => (
        <button key={item} className={index === 0 ? 'active' : ''} type="button">{item}</button>
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
