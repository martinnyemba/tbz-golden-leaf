// screens-auth-home.jsx
// Auth, onboarding, home dashboard, search, notifications, profile, static pages.

// ─────────────────────────────────────────────────────────────
// 1. ONBOARDING
// ─────────────────────────────────────────────────────────────
function ScreenOnboarding({ navigate }) {
  const { c } = useGL();
  const [step, setStep] = React.useState(0);
  const slides = [
    {
      title: 'Welcome to Golden Leaf',
      sub: 'Your TBZ field companion for grower registration, inspection, and permit management — built for the road, online or off.',
      art: 'leaf',
    },
    {
      title: 'Inspect anywhere, sync later',
      sub: 'Capture nursery, field and curing inspections offline. Reports queue safely until you are back online.',
      art: 'inspection',
    },
    {
      title: 'Permits in your pocket',
      sub: 'Request, validate and issue transport permits. Scan QR codes at sales floors with confidence.',
      art: 'permit',
    },
  ];
  const isLast = step === slides.length - 1;
  return (
    <Screen padded={false}>
      <div style={{ padding: '20px 20px 0', display: 'flex', justifyContent: 'flex-end' }}>
        <button onClick={() => navigate('login')} style={{
          background: 'none', border: 'none', color: c.textMuted, fontSize: 14,
          fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit',
        }}>Skip</button>
      </div>
      <div style={{ padding: '32px 28px 16px', display: 'flex', justifyContent: 'center' }}>
        <TbzMark size={120} />
      </div>
      <div style={{ padding: '20px 28px', textAlign: 'center' }}>
        <div style={{ fontSize: 26, fontWeight: 800, color: c.text, lineHeight: 1.2, letterSpacing: -0.3 }}>
          {slides[step].title}
        </div>
        <div style={{ fontSize: 15, color: c.textMuted, marginTop: 14, lineHeight: 1.5 }}>
          {slides[step].sub}
        </div>
      </div>
      <div style={{ flex: 1 }} />
      <div style={{ padding: '0 28px 28px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 24 }}>
          {slides.map((_, i) => (
            <div key={i} style={{
              width: i === step ? 24 : 8, height: 8, borderRadius: 4,
              background: i === step ? c.primary : c.outline,
              transition: 'all 0.2s',
            }} />
          ))}
        </div>
        <Button onClick={() => isLast ? navigate('login') : setStep(step + 1)}
                iconRight={<Icon name="arrow-right" size={20} />}>
          {isLast ? 'Get Started' : 'Next'}
        </Button>
        {step > 0 && (
          <Button variant="ghost" onClick={() => setStep(step - 1)} style={{ marginTop: 8 }}>Back</Button>
        )}
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// 2. PORTAL LOGIN
// ─────────────────────────────────────────────────────────────
function ScreenLogin({ navigate }) {
  const { c } = useGL();
  const [showPass, setShowPass] = React.useState(false);
  const [showApi, setShowApi] = React.useState(false);
  const [email, setEmail] = React.useState('inspector.banda@tbz.org.zm');
  const [pass, setPass] = React.useState('••••••••••');
  return (
    <Screen padded={false}>
      <div style={{ padding: '32px 28px 16px', display: 'flex', alignItems: 'center', gap: 14 }}>
        <TbzMark size={56} />
        <div>
          <div style={{ fontSize: 11, fontWeight: 700, color: c.gold, letterSpacing: 1, textTransform: 'uppercase' }}>
            Tobacco Board of Zambia
          </div>
          <div style={{ fontSize: 18, fontWeight: 800, color: c.text }}>Golden Leaf</div>
        </div>
      </div>
      <div style={{ padding: '16px 28px 8px' }}>
        <div style={{ fontSize: 26, fontWeight: 800, color: c.text, letterSpacing: -0.3 }}>Sign in</div>
        <div style={{ fontSize: 14, color: c.textMuted, marginTop: 6 }}>
          Use your TRMCS portal credentials to continue.
        </div>
      </div>
      <div style={{ padding: '20px 28px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Input label="Email" value={email} onChange={(e) => setEmail(e.target.value)}
               icon={<Icon name="mail" size={20} />} placeholder="you@tbz.org.zm" />
        <Input label="Password" value={pass} onChange={(e) => setPass(e.target.value)}
               type={showPass ? 'text' : 'password'} icon={<Icon name="lock" size={20} />}
               suffix={<button onClick={() => setShowPass(!showPass)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: c.textMuted, padding: 4 }}>
                 <Icon name={showPass ? 'eye-off' : 'eye'} size={18} />
               </button>}
               placeholder="Enter password" />
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
          <button onClick={() => setShowApi(!showApi)} style={{
            background: 'none', border: 'none', color: c.textMuted, fontSize: 12,
            fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit', display: 'flex', alignItems: 'center', gap: 4,
          }}>
            <Icon name="sliders" size={14} /> API settings
          </button>
          <button onClick={() => navigate('forgot-password')} style={{ background: 'none', border: 'none', color: c.primary, fontSize: 12, fontWeight: 700, cursor: 'pointer', fontFamily: 'inherit' }}>
            Forgot password?
          </button>
        </div>
        {showApi && (
          <Card padding={14} style={{ background: c.surfaceAlt, border: 'none' }}>
            <Input label="Portal Base URL" value="https://portal.tbz.org.zm" icon={<Icon name="cloud" size={18} />} />
            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              <Button variant="outline" size="sm">Test connection</Button>
              <Pill tone="success" size="sm" icon={<span style={{ width: 6, height: 6, borderRadius: '50%', background: c.success }} />}>
                Reachable · 84 ms
              </Pill>
            </div>
          </Card>
        )}
        <Button onClick={() => navigate('login-2fa')} style={{ marginTop: 6 }}>Sign In</Button>
        <div style={{ fontSize: 12, color: c.textSubtle, textAlign: 'center', marginTop: 8 }}>
          By continuing you accept the TBZ <span style={{ color: c.primary, fontWeight: 600 }}>Terms</span> and <span style={{ color: c.primary, fontWeight: 600 }}>Privacy Policy</span>.
        </div>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// 3. LOGIN 2FA
// ─────────────────────────────────────────────────────────────
function ScreenLogin2FA({ navigate }) {
  const { c } = useGL();
  const [code, setCode] = React.useState(['1', '4', '8', '', '', '']);
  return (
    <Screen padded={false}>
      <ScreenHeader title="" onBack={() => navigate('login')} />
      <div style={{ padding: '8px 28px 24px' }}>
        <div style={{
          width: 64, height: 64, borderRadius: 20, background: c.primarySoft, color: c.primary,
          display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16,
        }}>
          <Icon name="shield-check" size={32} />
        </div>
        <div style={{ fontSize: 24, fontWeight: 800, color: c.text }}>Two-factor verification</div>
        <div style={{ fontSize: 14, color: c.textMuted, marginTop: 8 }}>
          Enter the 6-digit code from your authenticator app.
        </div>
        <div style={{ display: 'flex', gap: 10, marginTop: 28, justifyContent: 'space-between' }}>
          {code.map((v, i) => (
            <div key={i} style={{
              flex: 1, height: 60, background: v ? c.primarySoft : c.surfaceAlt,
              border: `1.5px solid ${v ? c.primary : 'transparent'}`,
              borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 22, fontWeight: 800, color: v ? c.primary : c.placeholder,
              fontFamily: 'ui-monospace, monospace',
            }}>{v || '·'}</div>
          ))}
        </div>
        <div style={{ fontSize: 12, color: c.textMuted, marginTop: 16 }}>
          Didn't get a code? <span style={{ color: c.primary, fontWeight: 700 }}>Resend</span> · Available in 24s
        </div>
        <Button onClick={() => navigate('home')} style={{ marginTop: 28 }}>Verify &amp; Continue</Button>
        <Button variant="ghost" style={{ marginTop: 8 }}>Use a recovery code</Button>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// 4. HOME DASHBOARD — Chefio-structured layout
// ─────────────────────────────────────────────────────────────
function ScreenHome({ navigate }) {
  const { c } = useGL();
  const [activeTab, setActiveTab] = React.useState('Today');
  const [activeFilter, setActiveFilter] = React.useState('All');

  const scheduleItems = {
    'Today': [
      { name: 'Mary Phiri', id: 'TBZ-04412', kind: 'Field', risk: 'High', time: '09:30', loc: 'Chadiza', tone: 'warning' },
      { name: 'Charles Tembo', id: 'TBZ-03128', kind: 'Curing', risk: 'Low', time: '13:00', loc: 'Katete', tone: 'primary' },
      { name: 'Felix Sakala', id: 'TBZ-04501', kind: 'Nursery', risk: 'Low', time: '15:30', loc: 'Petauke', tone: 'primary' },
    ],
    'This week': [
      { name: 'Gladys Mwale', id: 'TBZ-02981', kind: 'Validation', risk: 'Med', time: 'Thu', loc: 'Lundazi', tone: 'gold' },
      { name: 'Loveness Banda', id: 'TBZ-04610', kind: 'Field', risk: 'Med', time: 'Fri', loc: 'Chipata', tone: 'gold' },
    ],
  };
  const filters = ['All', 'Field', 'Nursery', 'Curing', 'Validation'];
  const items = scheduleItems[activeTab] || [];
  const visible = activeFilter === 'All' ? items : items.filter(i => i.kind === activeFilter);

  return (
    <Screen padded={false}>
      {/* Compact header */}
      <div style={{ padding: '14px 20px 10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <TbzMark size={36} />
          <div>
            <div style={{ fontSize: 13, color: c.textMuted }}>Good morning,</div>
            <div style={{ fontSize: 16, fontWeight: 800, color: c.text }}>Inspector Banda</div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <SyncChip status="pending" count={4} />
          <button onClick={() => navigate('notifications')} style={{
            width: 40, height: 40, borderRadius: '50%', background: c.surfaceAlt,
            border: 'none', color: c.text, cursor: 'pointer', position: 'relative',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="bell" size={20} />
            <span style={{
              position: 'absolute', top: 6, right: 6, width: 14, height: 14, borderRadius: '50%',
              background: c.gold, color: '#1A1308', fontSize: 8, fontWeight: 800,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>3</span>
          </button>
        </div>
      </div>

      {/* Search bar */}
      <div style={{ padding: '0 20px 14px' }}>
        <SearchBar placeholder="Search growers, permits, NRC..."
                   onScan={() => navigate('permit-validate')} />
      </div>

      {/* Module quick-access icons */}
      <div style={{ padding: '0 20px 6px' }}>
        <div style={{ fontSize: 17, fontWeight: 700, color: c.text, marginBottom: 12 }}>Module</div>
        <div style={{ display: 'flex', gap: 14, overflowX: 'auto', scrollbarWidth: 'none', paddingBottom: 4 }}>
          {[
            { id: 'registration', label: 'Growers', icon: 'users', badge: null },
            { id: 'permits', label: 'Permits', icon: 'permit', badge: null },
            { id: 'inspection', label: 'Inspect', icon: 'inspection', badge: null },
            { id: 'marketing', label: 'Sales', icon: 'bale', badge: 3 },
            { id: 'corrections', label: 'Corrections', icon: 'warning', badge: 3 },
            { id: 'arbitration', label: 'Arbitration', icon: 'gavel', badge: null },
          ].map((it) => (
            <button key={it.id} onClick={() => navigate(it.id)} style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6,
              background: 'none', border: 'none', cursor: 'pointer', flexShrink: 0, padding: 0,
            }}>
              <div style={{
                width: 52, height: 52, borderRadius: 18,
                background: c.surfaceAlt, color: c.primary,
                display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative',
              }}>
                <Icon name={it.icon} size={24} />
                {it.badge && (
                  <span style={{
                    position: 'absolute', top: -4, right: -4, width: 18, height: 18,
                    borderRadius: '50%', background: c.gold, color: '#1A1308',
                    fontSize: 10, fontWeight: 800,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>{it.badge}</span>
                )}
              </div>
              <span style={{ fontSize: 11, fontWeight: 600, color: c.textMuted, whiteSpace: 'nowrap' }}>{it.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Divider */}
      <div style={{ height: 8, background: c.surfaceMuted, margin: '10px 0' }} />

      {/* Tab strip */}
      <div style={{ display: 'flex', padding: '0 20px', borderBottom: `1px solid ${c.outlineSoft}` }}>
        {['Today', 'This week'].map((t) => (
          <button key={t} onClick={() => setActiveTab(t)} style={{
            flex: 1, padding: '10px 0', border: 'none', background: 'none', cursor: 'pointer',
            fontFamily: 'inherit', fontSize: 14,
            fontWeight: activeTab === t ? 700 : 500,
            color: activeTab === t ? c.primary : c.textMuted,
            borderBottom: `2.5px solid ${activeTab === t ? c.primary : 'transparent'}`,
            marginBottom: -1,
          }}>{t}</button>
        ))}
      </div>

      {/* Type filter pills */}
      <div style={{ padding: '10px 20px 4px' }}>
        <FilterPills options={filters} value={activeFilter} onChange={setActiveFilter} />
      </div>

      {/* 2-column schedule card grid */}
      <div style={{ padding: '8px 20px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {visible.map((it, i) => (
          <Card key={i} padding={12} onClick={() => navigate('inspection-detail')} elevated>
            <div style={{ marginBottom: 8 }}>
              <Pill tone={it.tone} size="sm">{it.kind}</Pill>
            </div>
            <div style={{ fontSize: 14, fontWeight: 700, color: c.text, lineHeight: 1.2 }}>{it.name}</div>
            <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace', marginTop: 3 }}>{it.id}</div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 }}>
              <span style={{ fontSize: 11, color: c.textMuted, display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                <Icon name="clock" size={11} /> {it.time}
              </span>
              <span style={{ fontSize: 11, color: c.textMuted, display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                <Icon name="map-pin" size={11} /> {it.loc}
              </span>
            </div>
            {it.risk === 'High' && (
              <div style={{ marginTop: 8 }}>
                <Pill tone="warning" size="sm" icon={<Icon name="warning" size={10} />}>High risk</Pill>
              </div>
            )}
          </Card>
        ))}
        {visible.length === 0 && (
          <div style={{ gridColumn: '1/-1', padding: '24px 0', textAlign: 'center', color: c.textMuted, fontSize: 13 }}>
            No {activeFilter === 'All' ? '' : activeFilter + ' '}inspections {activeTab.toLowerCase()}.
          </div>
        )}
      </div>
      <div style={{ height: 12 }} />
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// SEARCH
// ─────────────────────────────────────────────────────────────
function ScreenSearch({ navigate }) {
  const { c } = useGL();
  const [q, setQ] = React.useState('');
  const recents = ['Mary Phiri', 'TBZ-2024-04412', 'PRM-9821', 'Chadiza'];
  const results = [
    { kind: 'grower', title: 'Mary Phiri', sub: 'TBZ-2024-04412 · Chadiza · NRC 224018/61/1', icon: 'profile' },
    { kind: 'grower', title: 'Mary Phiri Banda', sub: 'TBZ-2024-04401 · Lundazi · NRC 224018/63/1', icon: 'profile' },
    { kind: 'permit', title: 'PRM-9821', sub: 'Mary Phiri · 24 bales · 712 kg · Active', icon: 'permit' },
  ];
  return (
    <Screen padded={false}>
      <div style={{ padding: '14px 20px 6px', display: 'flex', alignItems: 'center', gap: 10 }}>
        <button onClick={() => navigate('home')} style={{
          width: 40, height: 40, borderRadius: '50%', background: c.surfaceAlt,
          border: 'none', color: c.text, cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon name="arrow-left" size={20} />
        </button>
        <div style={{ flex: 1 }}>
          <SearchBar value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search growers, permits, NRC…" />
        </div>
      </div>
      <div style={{ padding: '8px 20px' }}>
        <FilterPills options={['All', 'Growers', 'Permits', 'Inspections']} value="All" onChange={() => {}} />
      </div>
      {!q ? (
        <div style={{ padding: '8px 20px' }}>
          <SectionHeader title="Recent" action="Clear" />
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {recents.map((r) => (
              <Row key={r} icon={<Icon name="clock" size={18} />} title={r} onClick={() => setQ(r)} />
            ))}
          </div>
          <SectionHeader title="Suggestions" style={{ marginTop: 8 }} />
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {['High-risk growers', 'Pending permits', 'Today’s inspections', 'Sales last 7 days'].map(t => (
              <Pill key={t} tone="default">{t}</Pill>
            ))}
          </div>
        </div>
      ) : (
        <div style={{ padding: '8px 20px' }}>
          <SectionHeader title={`${results.length} results`} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {results.map((r, i) => (
              <Card key={i} padding={12} onClick={() => navigate(r.kind === 'permit' ? 'permit-detail' : 'grower-details')}>
                <Row icon={<Icon name={r.icon} size={20} />} title={r.title} subtitle={r.sub}
                     tone={r.kind === 'permit' ? 'gold' : 'primary'}
                     right={<Icon name="chevron-right" size={18} style={{ color: c.textSubtle }}/>} />
              </Card>
            ))}
          </div>
        </div>
      )}
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// NOTIFICATIONS
// ─────────────────────────────────────────────────────────────
function ScreenNotifications({ navigate }) {
  const { c } = useGL();
  const [filter, setFilter] = React.useState('All');
  const items = [
    { tone: 'primary', icon: 'shield-check', title: 'Permit PRM-9821 approved', sub: 'Valid from 12 May to 19 May. 24 bales · 712 kg.', time: '12m', unread: true, ref: 'PRM-9821' },
    { tone: 'gold', icon: 'cloud-up', title: 'Sync queued', sub: '4 inspection reports waiting for connection.', time: '1h', unread: true, ref: 'Queue' },
    { tone: 'danger', icon: 'warning', title: 'High-risk grower flagged', sub: 'Mary Phiri (TBZ-04412) — please conduct field inspection.', time: '3h', unread: true, ref: 'TBZ-04412' },
    { tone: 'info', icon: 'calendar', title: 'New inspection scheduled', sub: 'Charles Tembo · Katete · Tomorrow at 13:00.', time: 'Yesterday', unread: false, ref: 'INS-771' },
    { tone: 'default', icon: 'document', title: 'Marketing brief updated', sub: 'TBZ 2025 marketing guidelines now available.', time: '2d', unread: false, ref: 'Guidelines' },
  ];
  return (
    <Screen padded={false}>
      <ScreenHeader title="Notifications" subtitle="3 unread" onBack={() => navigate('home')}
                    right={<button style={{ background: 'none', border: 'none', color: c.primary, fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>Mark all</button>} />
      <div style={{ padding: '8px 20px' }}>
        <SearchBar placeholder="Search notifications" />
      </div>
      <div style={{ padding: '8px 20px' }}>
        <FilterPills options={['All', 'Unread', 'Favorite', 'Archive']} value={filter} onChange={setFilter} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {items.map((n, i) => (
          <Card key={i} padding={14} accent={n.unread ? (n.tone === 'gold' ? 'gold' : 'primary') : undefined}>
            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{
                width: 40, height: 40, borderRadius: 12, flexShrink: 0,
                background: c[`${n.tone === 'default' ? 'surfaceAlt' : n.tone + 'Soft'}`] || c.surfaceAlt,
                color: c[n.tone] || c.text,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={n.icon} size={20} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{n.title}</div>
                  <div style={{ fontSize: 11, color: c.textSubtle, flexShrink: 0 }}>{n.time}</div>
                </div>
                <div style={{ fontSize: 12, color: c.textMuted, marginTop: 3, lineHeight: 1.4 }}>{n.sub}</div>
                <div style={{ marginTop: 8 }}>
                  <Pill tone="default" size="sm">Ref · {n.ref}</Pill>
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// PROFILE
// ─────────────────────────────────────────────────────────────
function ScreenProfile({ navigate, dark, setTweak }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <div style={{
        background: `linear-gradient(180deg, ${c.primaryDeep} 0%, ${c.primary} 100%)`,
        padding: '20px 20px 60px', color: '#fff',
      }}>
        <ScreenHeader title="Profile" sticky={false}
                      onBack={() => navigate('home')}
                      right={<button style={{ background: 'rgba(255,255,255,0.15)', border: 'none', color: '#fff', borderRadius: 999, padding: '6px 12px', fontSize: 12, fontWeight: 700, cursor: 'pointer' }}>Edit</button>} />
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 4px 0' }}>
          <Avatar name="Joseph Banda" size={64} gold />
          <div>
            <div style={{ fontSize: 20, fontWeight: 800 }}>Joseph Banda</div>
            <div style={{ fontSize: 13, opacity: 0.85 }}>joseph.banda@tbz.org.zm</div>
            <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
              <Pill tone="gold" size="sm">Inspector</Pill>
              <Pill tone="outline" size="sm" style={{ color: '#fff', borderColor: 'rgba(255,255,255,0.3)', background: 'rgba(255,255,255,0.1)' }}>Eastern</Pill>
            </div>
          </div>
        </div>
      </div>
      <div style={{ padding: '0 20px', marginTop: -44 }}>
        <Card elevated padding={4}>
          <Row icon={<Icon name={dark ? 'moon' : 'sun'} size={20} />} title="Dark mode"
               subtitle="Outdoor glare reduction" right={<Toggle value={dark} onChange={(v) => setTweak('dark', v)} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="lock" size={20} />} title="Change password"
               onClick={() => navigate('change-password')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="shield-check" size={20} />} title="Two-factor auth"
               subtitle="Authenticator enabled" tone="primary"
               right={<Pill tone="success" size="sm">On</Pill>}
               onClick={() => navigate('2fa-settings')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="sync" size={20} />} title="Sync settings"
               subtitle="Wi-Fi only · Auto sync on" onClick={() => navigate('sync-settings')} />
        </Card>
      </div>
      <div style={{ padding: '14px 20px' }}>
        <SectionHeader title="Information" />
        <Card padding={4}>
          <Row icon={<Icon name="info" size={20} />} title="About app" onClick={() => navigate('about')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="document" size={20} />} title="TBZ regulatory guidelines" onClick={() => navigate('guidelines')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="document" size={20} />} title="Terms & conditions" onClick={() => navigate('terms')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="shield" size={20} />} title="Privacy policy" onClick={() => navigate('privacy')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="share" size={20} />} title="Share this app" />
        </Card>
      </div>
      <div style={{ padding: '14px 20px 24px' }}>
        <Button variant="dangerOutline" icon={<Icon name="logout" size={18} />} onClick={() => navigate('login')}>
          Log out
        </Button>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// CHANGE PASSWORD
// ─────────────────────────────────────────────────────────────
function ScreenChangePassword({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={<Button onClick={() => navigate('profile')}>Update password</Button>}>
      <ScreenHeader title="Change password" onBack={() => navigate('profile')} />
      <div style={{ padding: '12px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Banner tone="info" icon={<Icon name="info" size={18} />} title="Strong password requirements"
                sub="At least 10 characters; must meet TBZ password policy (mix of letters, numbers, and a symbol)." />
        <Input label="Current password" type="password" icon={<Icon name="lock" size={20} />} placeholder="••••••••" required />
        <Input label="New password" type="password" icon={<Icon name="lock" size={20} />} placeholder="At least 8 characters" required helper="Strength: strong" success />
        <Input label="Confirm new password" type="password" icon={<Icon name="lock" size={20} />} placeholder="Repeat new password" required />
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// 2FA SETTINGS
// ─────────────────────────────────────────────────────────────
function Screen2FASettings({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="Two-factor auth" onBack={() => navigate('profile')} />
      <div style={{ padding: '12px 20px' }}>
        <Card padding={20} accent="primary">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <Icon name="shield-check" size={22} style={{ color: c.primary }} />
            <div style={{ fontSize: 16, fontWeight: 800, color: c.text }}>2FA is enabled</div>
            <Pill tone="success" size="sm">On</Pill>
          </div>
          <div style={{ fontSize: 13, color: c.textMuted }}>
            Your account is protected by an authenticator app. You'll need a 6-digit code to sign in.
          </div>
        </Card>
        <SectionHeader title="Setup" style={{ marginTop: 20 }} />
        <Card padding={20} style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
          <QRPlaceholder size={120} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 12, color: c.textMuted }}>Manual setup key</div>
            <div style={{ fontSize: 13, fontWeight: 700, fontFamily: 'ui-monospace, monospace', color: c.text, wordBreak: 'break-all', marginTop: 4 }}>
              JBSWY3DPEHPK3PXP
            </div>
            <button style={{ background: 'none', border: 'none', color: c.primary, fontSize: 12, fontWeight: 700, cursor: 'pointer', marginTop: 8, padding: 0 }}>
              Copy key
            </button>
          </div>
        </Card>
        <SectionHeader title="Recovery codes" style={{ marginTop: 20 }} />
        <Card padding={16}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            {['8X3K-Q2P9', '4M2H-J8C1', '7B5N-W1F4', 'P9KZ-D6T2', 'V3RJ-9S7M', 'L8TG-N4QA'].map(code => (
              <div key={code} style={{
                background: c.surfaceAlt, padding: '8px 12px', borderRadius: 10,
                fontFamily: 'ui-monospace, monospace', fontSize: 13, color: c.text,
              }}>{code}</div>
            ))}
          </div>
          <Button variant="ghost" size="sm" full={false} style={{ marginTop: 12 }}>Regenerate codes</Button>
        </Card>
        <Button variant="dangerOutline" style={{ marginTop: 20 }}>Disable 2FA</Button>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// SYNC SETTINGS
// ─────────────────────────────────────────────────────────────
function ScreenSyncSettings({ navigate }) {
  const { c } = useGL();
  const [auto, setAuto] = React.useState(true);
  const [wifi, setWifi] = React.useState(true);
  return (
    <Screen padded={false}>
      <ScreenHeader title="Sync settings" onBack={() => navigate('profile')} />
      <div style={{ padding: '12px 20px' }}>
        <Card padding={18} elevated>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 48, height: 48, borderRadius: 14, background: c.primarySoft, color: c.primary,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="cloud-up" size={24} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 16, fontWeight: 800, color: c.text }}>4 records pending</div>
              <div style={{ fontSize: 12, color: c.textMuted }}>Last sync · 2 hours ago</div>
            </div>
            <SyncChip status="pending" count={4} />
          </div>
          <Button style={{ marginTop: 14 }} icon={<Icon name="sync" size={18} />}>Sync now</Button>
        </Card>
        <SectionHeader title="Preferences" style={{ marginTop: 20 }} />
        <Card padding={4}>
          <Row icon={<Icon name="sync" size={20} />} title="Automatic sync"
               subtitle="Keep data fresh while working" right={<Toggle value={auto} onChange={setAuto} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="cloud" size={20} />} title="Sync only on Wi-Fi"
               subtitle="Skip cellular / metered networks" right={<Toggle value={wifi} onChange={setWifi} />} />
        </Card>
        <SectionHeader title="Pending by type" style={{ marginTop: 20 }} />
        <Card padding={4}>
          <Row icon={<Icon name="users" size={20} />} title="Grower registrations" subtitle="2 pending · 0 failed"
               right={<Pill tone="pending" size="sm">2</Pill>} tone="primary" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="clipboard" size={20} />} title="Inspection reports" subtitle="1 pending · 1 needs review"
               right={<Pill tone="pending" size="sm">2</Pill>} tone="primary" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="bale" size={20} />} title="Sales batches" subtitle="0 pending" tone="gold"
               right={<Pill tone="success" size="sm">Synced</Pill>} />
        </Card>
        <SectionHeader title="Connection" style={{ marginTop: 20 }} />
        <Card padding={14}>
          <FieldRow label="Portal" value="portal.tbz.org.zm" mono />
          <FieldRow label="Status" value={<Pill tone="success" size="sm" icon={<span style={{ width: 6, height: 6, borderRadius: '50%', background: c.success }} />}>Reachable · 84 ms</Pill>} />
          <FieldRow label="Network" value="Wi-Fi · TBZ-Office" />
          <Button variant="ghost" size="sm" full={false} style={{ marginTop: 8 }}>Portal settings</Button>
        </Card>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// STATIC PAGES — About, Guidelines, Terms, Privacy
// ─────────────────────────────────────────────────────────────
function ScreenAbout({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="About" onBack={() => navigate('profile')} />
      <div style={{ padding: '24px 20px', textAlign: 'center' }}>
        <TbzMark size={88} />
        <div style={{ fontSize: 22, fontWeight: 800, color: c.text, marginTop: 14 }}>Golden Leaf</div>
        <div style={{ fontSize: 13, color: c.textMuted, marginTop: 4 }}>Version 2.4.1 · build 240412</div>
        <Pill tone="gold" size="sm" style={{ marginTop: 14 }}>Tobacco Board of Zambia</Pill>
      </div>
      <div style={{ padding: '4px 20px 16px' }}>
        <SectionHeader title="About TRMCS" />
        <Card padding={16}>
          <div style={{ fontSize: 13, color: c.text, lineHeight: 1.55 }}>
            The Tobacco Regulation, Marketing and Compliance System (TRMCS) is the Tobacco Board of Zambia's
            integrated platform for the registration of growers, inspection of crops, issuance of transport permits,
            and capture of sales floor activity.
          </div>
        </Card>
        <SectionHeader title="Key features" style={{ marginTop: 16 }} />
        <Card padding={4}>
          {[
            { icon: 'users', t: 'Grower registration', s: 'Onboard small-scale, commercial, and company growers.' },
            { icon: 'inspection', t: 'Field inspections', s: 'Nursery, field, curing, and validation reporting.' },
            { icon: 'permit', t: 'Transport permits', s: 'Request, validate, approve, and track movement permits.' },
            { icon: 'bale', t: 'Sales capture', s: 'Bale-level sales recording at sales floors with QR validation.' },
            { icon: 'cloud-up', t: 'Offline first', s: 'Work in the field without a connection — sync when ready.' },
          ].map((f, i) => (
            <React.Fragment key={i}>
              {i > 0 && <div style={{ height: 1, background: c.outlineSoft }} />}
              <Row icon={<Icon name={f.icon} size={20} />} title={f.t} subtitle={f.s} tone="primary" />
            </React.Fragment>
          ))}
        </Card>
      </div>
    </Screen>
  );
}

function ScreenGuidelines({ navigate }) {
  const { c } = useGL();
  const sections = [
    { tone: 'primary', icon: 'users', t: 'Registration', items: [
      'Every grower must be registered with TBZ before the marketing season begins.',
      'NRC, GPS coordinates, and farm photographs are mandatory for new registrations.',
      'Renewals must be completed annually and validated by the assigned inspector.',
    ]},
    { tone: 'gold', icon: 'truck', t: 'Movement & permits', items: [
      'A valid transport permit is required for any tobacco moving between districts.',
      'QR codes must be scanned at every sales floor checkpoint.',
      'Group permits require at least 2 grower entries.',
    ]},
    { tone: 'info', icon: 'bale', t: 'Marketing', items: [
      'Bales must carry tickets matching the registered grower.',
      'Sales must be captured at registered sales floors only.',
      'Rejected bales must include a documented rejection reason.',
    ]},
  ];
  return (
    <Screen padded={false}>
      <ScreenHeader title="TBZ guidelines" onBack={() => navigate('profile')} />
      <div style={{ padding: '12px 20px' }}>
        {sections.map((s, i) => (
          <div key={i} style={{ marginBottom: 16 }}>
            <Card padding={16}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                <div style={{
                  width: 36, height: 36, borderRadius: 12,
                  background: c[`${s.tone}Soft`], color: c[s.tone],
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}><Icon name={s.icon} size={18} /></div>
                <div style={{ fontSize: 15, fontWeight: 800, color: c.text }}>{s.t}</div>
              </div>
              <ul style={{ margin: 0, padding: '0 0 0 8px', listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 8 }}>
                {s.items.map((it, j) => (
                  <li key={j} style={{ display: 'flex', gap: 10, fontSize: 13, color: c.text, lineHeight: 1.5 }}>
                    <span style={{ color: c[s.tone], flexShrink: 0, marginTop: 2 }}>
                      <Icon name="check-circle" size={16} />
                    </span>
                    <span>{it}</span>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        ))}
      </div>
    </Screen>
  );
}

function ScreenTerms({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="Terms & conditions" onBack={() => navigate('profile')} />
      <div style={{ padding: '12px 20px', fontSize: 13, color: c.text, lineHeight: 1.55 }}>
        <Card padding={16}>
          <div style={{ fontSize: 11, color: c.textMuted, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.5 }}>Effective 12 March 2025</div>
          <div style={{ fontSize: 16, fontWeight: 800, color: c.text, marginTop: 6 }}>Use of the Golden Leaf application</div>
          <div style={{ marginTop: 10 }}>
            By using this application you agree to act in accordance with the Tobacco Act of Zambia
            and all guidelines issued by the Tobacco Board of Zambia. The data you capture is the
            property of TBZ and may not be reproduced without authorisation.
          </div>
        </Card>
        <SectionHeader title="Sections" style={{ marginTop: 16 }} />
        <Card padding={4}>
          {['1. Eligibility & accounts', '2. Permitted use', '3. Data & privacy', '4. Offline cache & sync',
            '5. Sanctions & enforcement', '6. Limitation of liability', '7. Updates to these terms'].map((t, i, a) => (
            <React.Fragment key={t}>
              <Row title={t} icon={<Icon name="document" size={18} />} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>
      </div>
    </Screen>
  );
}

function ScreenPrivacy({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="Privacy policy" onBack={() => navigate('profile')} />
      <div style={{ padding: '12px 20px', fontSize: 13, color: c.text, lineHeight: 1.55 }}>
        <Card padding={16}>
          <div style={{ fontSize: 16, fontWeight: 800, color: c.text }}>How we handle your data</div>
          <div style={{ marginTop: 10, color: c.textMuted }}>
            The TBZ collects only the personal information necessary to register growers, conduct
            inspections and manage permits. Data captured offline is encrypted on the device and
            transmitted over TLS once a connection is available.
          </div>
        </Card>
        <SectionHeader title="What we collect" style={{ marginTop: 16 }} />
        <Card padding={4}>
          {[
            { i: 'profile', t: 'Identity', s: 'NRC, name, contact details for verified growers and inspectors.' },
            { i: 'gps', t: 'Location', s: 'GPS only when capturing inspection / registration records.' },
            { i: 'image', t: 'Photographs', s: 'ID and farm photographs as required by law.' },
            { i: 'database', t: 'Telemetry', s: 'Anonymous app diagnostics to improve reliability.' },
          ].map((f, i, a) => (
            <React.Fragment key={f.t}>
              <Row icon={<Icon name={f.i} size={20} />} title={f.t} subtitle={f.s} tone="primary" />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>
      </div>
    </Screen>
  );
}

function ScreenMenu({ navigate }) {
  const { c } = useGL();
  return (
    <div style={{ position: 'absolute', inset: 0, background: 'rgba(15,30,18,0.55)', zIndex: 100,
                  display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}
         onClick={() => navigate('home')}>
      <div onClick={(e) => e.stopPropagation()} style={{
        background: c.surface, borderTopLeftRadius: 28, borderTopRightRadius: 28,
        padding: '8px 16px 24px',
      }}>
        <div style={{ width: 40, height: 4, background: c.outline, borderRadius: 2,
                      margin: '8px auto 16px' }} />
        <div style={{ fontSize: 18, fontWeight: 800, color: c.text, padding: '0 8px 12px' }}>More options</div>
        <Card padding={4}>
          <Row icon={<Icon name="sync" size={20} />} title="Sync settings" subtitle="4 records pending" tone="primary" onClick={() => navigate('sync-settings')} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="help" size={20} />} title="Help & support" subtitle="Contact TBZ field operations" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="info" size={20} />} title="About application" onClick={() => navigate('about')} />
        </Card>
        <Button variant="ghost" style={{ marginTop: 14 }} onClick={() => navigate('home')}>Close</Button>
      </div>
    </div>
  );
}

Object.assign(window, {
  ScreenOnboarding, ScreenLogin, ScreenLogin2FA, ScreenHome,
  ScreenSearch, ScreenNotifications, ScreenProfile, ScreenChangePassword,
  Screen2FASettings, ScreenSyncSettings, ScreenAbout, ScreenGuidelines,
  ScreenTerms, ScreenPrivacy, ScreenMenu,
});
