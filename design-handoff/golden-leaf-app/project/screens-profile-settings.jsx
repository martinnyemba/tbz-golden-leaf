// screens-profile-settings.jsx
// Profile/officer screens: profile, settings, notifications, sync, 2FA, audit log, search.

function ScreenProfile({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="Officer profile" onBack={() => navigate('home')}
                    right={<button onClick={() => navigate('settings')} style={{
                      background: 'transparent', border: 'none', color: c.text, cursor: 'pointer',
                    }}><Icon name="settings" size={20} /></button>} />
      <div style={{ padding: '8px 20px' }}>
        <Card padding={20} style={{
          background: `linear-gradient(135deg, ${c.primaryDeep} 0%, ${c.primary} 60%, ${c.primaryDeep} 100%)`,
          border: 'none', color: '#fff', overflow: 'hidden', position: 'relative',
        }}>
          <div style={{ position: 'absolute', right: -20, bottom: -20, opacity: 0.07, pointerEvents: 'none' }}>
            <TbzMark size={180} />
          </div>
          <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
            <Avatar name="Joseph Banda" size={68} gold />
            <div style={{ flex: 1, color: '#fff' }}>
              <div style={{ fontSize: 18, fontWeight: 800, color: '#fff' }}>Joseph Banda</div>
              <div style={{ fontSize: 12, opacity: 0.8, fontFamily: 'ui-monospace, monospace' }}>OFC-EAST-018</div>
              <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
                <Pill tone="gold" size="sm" icon={<Icon name="check" size={11} />}>Verified</Pill>
                <Pill tone="default" size="sm" style={{ background: 'rgba(255,255,255,0.2)', color: '#fff', border: 'none' }}>Senior</Pill>
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 14, marginTop: 18, paddingTop: 14, borderTop: '1px solid rgba(255,255,255,0.18)' }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 10, opacity: 0.7, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.4 }}>Region</div>
              <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>Eastern Province</div>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 10, opacity: 0.7, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.4 }}>Districts</div>
              <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>Chadiza · Katete</div>
            </div>
          </div>
        </Card>
      </div>

      <SectionHeader title="Season performance" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <Card padding={14}>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <div style={{ width: 36, height: 36, borderRadius: 12, background: c.primarySoft, color: c.primary,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="profile" size={18} />
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 800, color: c.text, lineHeight: 1 }}>247</div>
              <div style={{ fontSize: 11, color: c.textMuted, marginTop: 4 }}>Growers managed</div>
            </div>
          </div>
        </Card>
        <Card padding={14}>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <div style={{ width: 36, height: 36, borderRadius: 12, background: c.goldSoft, color: c.goldDeep,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="leaf" size={18} />
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 800, color: c.text, lineHeight: 1 }}>184</div>
              <div style={{ fontSize: 11, color: c.textMuted, marginTop: 4 }}>Inspections done</div>
            </div>
          </div>
        </Card>
        <Card padding={14}>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <div style={{ width: 36, height: 36, borderRadius: 12, background: c.successSoft, color: c.success,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="permit" size={18} />
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 800, color: c.text, lineHeight: 1 }}>312</div>
              <div style={{ fontSize: 11, color: c.textMuted, marginTop: 4 }}>Permits issued</div>
            </div>
          </div>
        </Card>
        <Card padding={14}>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <div style={{ width: 36, height: 36, borderRadius: 12, background: c.dangerSoft, color: c.danger,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="gavel" size={18} />
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 800, color: c.text, lineHeight: 1 }}>9</div>
              <div style={{ fontSize: 11, color: c.textMuted, marginTop: 4 }}>Disputes resolved</div>
            </div>
          </div>
        </Card>
      </div>

      <SectionHeader title="My account" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={4}>
          <Row icon={<Icon name="profile" size={20} />} title="Personal details"
               subtitle="Name, NRC, phone, email" tone="primary"
               onClick={() => navigate('settings')}
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="lock" size={20} />} title="Security & 2FA"
               subtitle="PIN, biometrics, recovery codes" tone="info"
               onClick={() => navigate('security')}
               right={<Pill size="sm" tone="success">On</Pill>} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="bell" size={20} />} title="Notifications"
               subtitle="Inspections, permits, disputes" tone="gold"
               onClick={() => navigate('notifications')}
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="cloud-up" size={20} />} title="Offline & sync"
               subtitle="Pending uploads · last sync 2 min ago" tone="primary"
               onClick={() => navigate('sync')}
               right={<Pill size="sm" tone="warning">3 pending</Pill>} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="document" size={20} />} title="Activity log"
               subtitle="Audit trail of your actions" tone="default"
               onClick={() => navigate('audit')}
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
        </Card>
      </div>

      <SectionHeader title="App" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px 0' }}>
        <Card padding={4}>
          <Row icon={<Icon name="globe" size={20} />} title="Language" subtitle="English (Zambia)"
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="moon" size={20} />} title="Appearance" subtitle="System default"
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="info" size={20} />} title="About" subtitle="v3.4.1 · TBZ-Field"
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
        </Card>
      </div>

      <div style={{ padding: '20px 20px 0' }}>
        <Button variant="dangerOutline" icon={<Icon name="logout" size={18} />} onClick={() => navigate('login')}>
          Sign out
        </Button>
      </div>
    </Screen>
  );
}

function ScreenSettings({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="Personal details" onBack={() => navigate('profile')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 0' }}>
          <Avatar name="Joseph Banda" size={84} gold />
          <button style={{
            marginTop: 10, background: c.surfaceAlt, border: 'none', borderRadius: 999,
            padding: '6px 14px', fontSize: 12, fontWeight: 700, color: c.primary, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', gap: 4,
          }}>
            <Icon name="camera" size={12} /> Change photo
          </button>
        </div>
        <Input label="Full name" value="Joseph Mwape Banda" />
        <Input label="Officer ID" value="OFC-EAST-018" disabled />
        <Input label="NRC" value="124018/61/1" />
        <Input label="Phone" value="+260 977 481 220" icon={<Icon name="phone" size={20} />} />
        <Input label="Email" value="j.banda@tbz.gov.zm" icon={<Icon name="mail" size={20} />} />
        <SectionHeader title="Assignment" />
        <Select label="Province" value="Eastern" options={['Eastern', 'Lusaka', 'Central', 'Southern', 'Northern']} disabled />
        <Select label="Districts" value="Chadiza · Katete" options={['Chadiza · Katete', 'Petauke · Lundazi']} />
        <SectionHeader title="Vehicle" />
        <Input label="Assigned vehicle" value="Toyota Hilux · ABG 5512" />
      </div>
      <div style={{ padding: '14px 20px 0' }}>
        <Button onClick={() => navigate('profile')}>Save changes</Button>
      </div>
    </Screen>
  );
}

function ScreenSecurity({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <ScreenHeader title="Security" onBack={() => navigate('profile')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Card padding={16} style={{ background: c.successSoft, border: `1px solid ${c.success}30` }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 44, height: 44, borderRadius: 14, background: c.success, color: '#fff',
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="shield-check" size={22} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 800, color: c.text }}>Account protected</div>
              <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>2FA + biometric · last login 09:42 today</div>
            </div>
          </div>
        </Card>

        <SectionHeader title="Authentication" />
        <Card padding={4}>
          <Row icon={<Icon name="key" size={20} />} title="PIN" subtitle="6 digits · changed 14 days ago" tone="primary"
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <ToggleRow icon={<Icon name="fingerprint" size={20} />} title="Biometric unlock"
                     subtitle="Use fingerprint to sign in" defaultOn />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <ToggleRow icon={<Icon name="lock" size={20} />} title="Two-factor authentication"
                     subtitle="SMS code on every new device" defaultOn />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="document" size={20} />} title="Recovery codes"
               subtitle="8 of 10 unused" tone="info"
               right={<Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />} />
        </Card>

        <SectionHeader title="Devices · 2 active" />
        <Card padding={4}>
          {[
            { d: 'Samsung A24', loc: 'This device · Chipata', t: '09:42 today', current: true },
            { d: 'Tecno Spark 9', loc: 'Lusaka · home', t: 'Yesterday 18:12', current: false },
          ].map((dv, i, a) => (
            <React.Fragment key={dv.d}>
              <Row icon={<Icon name="phone" size={20} />} title={dv.d}
                   subtitle={`${dv.loc} · ${dv.t}`} tone="primary"
                   right={dv.current
                     ? <Pill size="sm" tone="success">This device</Pill>
                     : <button style={{ background: 'none', border: 'none', color: c.danger, fontSize: 12, fontWeight: 700, cursor: 'pointer' }}>Revoke</button>} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>

        <SectionHeader title="Sensitive actions" />
        <Card padding={4}>
          <ToggleRow icon={<Icon name="permit" size={20} />} title="Re-enter PIN before issuing permits" defaultOn />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <ToggleRow icon={<Icon name="gavel" size={20} />} title="Re-enter PIN before resolving disputes" defaultOn />
        </Card>
      </div>
    </Screen>
  );
}

function ScreenNotifications({ navigate }) {
  const { c } = useGL();
  const items = [
    { kind: 'permit', title: 'Permit PRM-9809 awaiting approval', sub: 'Eastern Coop · 5 growers', t: '4m ago', tone: 'pending', unread: true },
    { kind: 'inspection', title: 'High-risk: Mary Phiri', sub: 'Risk score increased to 78', t: '38m ago', tone: 'gold', unread: true },
    { kind: 'sync', title: 'Sync complete', sub: '14 records uploaded', t: '1h ago', tone: 'success', unread: false },
    { kind: 'dispute', title: 'New dispute opened', sub: 'ARB-1182 · Bale count mismatch', t: '2h ago', tone: 'danger', unread: false },
    { kind: 'update', title: 'App updated to v3.4.1', sub: 'Curing barn module added', t: 'Yesterday', tone: 'info', unread: false },
  ];
  const map = { permit: 'permit', inspection: 'leaf', sync: 'cloud-up', dispute: 'gavel', update: 'star' };
  return (
    <Screen padded={false}>
      <ScreenHeader title="Notifications" subtitle="2 unread" onBack={() => navigate('profile')}
                    right={<button style={{ background: 'transparent', border: 'none', fontSize: 12, fontWeight: 700, color: c.primary, cursor: 'pointer' }}>Mark read</button>} />
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Permits', 'Inspections', 'Disputes', 'System']} value="All" onChange={() => {}} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {items.map((n, i) => (
          <Card key={i} padding={14} style={{ background: n.unread ? c.surface : c.surfaceAlt,
                                                borderColor: n.unread ? c.outlineSoft : 'transparent' }}>
            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{
                width: 40, height: 40, borderRadius: 12, flexShrink: 0,
                background: c[`${n.tone === 'pending' ? 'warning' : n.tone === 'danger' ? 'danger' : n.tone === 'gold' ? 'gold' : n.tone === 'success' ? 'success' : 'primary'}Soft`],
                color: c[n.tone === 'pending' ? 'goldDeep' : n.tone === 'danger' ? 'danger' : n.tone === 'gold' ? 'goldDeep' : n.tone === 'success' ? 'success' : 'primary'],
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={map[n.kind]} size={18} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{n.title}</div>
                  {n.unread && <div style={{ width: 8, height: 8, borderRadius: '50%', background: c.primary, flexShrink: 0, marginTop: 6 }} />}
                </div>
                <div style={{ fontSize: 12, color: c.textMuted, marginTop: 2 }}>{n.sub}</div>
                <div style={{ fontSize: 11, color: c.textSubtle, marginTop: 6 }}>{n.t}</div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </Screen>
  );
}

function ScreenSync({ navigate }) {
  const { c } = useGL();
  const items = [
    { t: 'Inspection report INS-2025-0412', s: 'Mary Phiri · 92 KB', icon: 'leaf', state: 'pending' },
    { t: 'Photos · row 11', s: '4 images · 3.2 MB', icon: 'camera', state: 'pending' },
    { t: 'Permit PRM-9821', s: 'Mary Phiri · 18 KB', icon: 'permit', state: 'pending' },
    { t: 'Sale capture SAL-2034', s: '23 bales · 24 KB', icon: 'bale', state: 'queued' },
  ];
  return (
    <Screen padded={false} footer={
      <Button icon={<Icon name="cloud-up" size={18} />}>Sync now</Button>
    }>
      <ScreenHeader title="Offline & sync" onBack={() => navigate('profile')} />
      <div style={{ padding: '8px 20px' }}>
        <Card padding={20} style={{
          background: `linear-gradient(135deg, ${c.primaryDeep} 0%, ${c.primary} 100%)`,
          border: 'none', color: '#fff',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div style={{ fontSize: 11, opacity: 0.7, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.4 }}>Last sync</div>
              <div style={{ fontSize: 22, fontWeight: 800, marginTop: 4 }}>2 min ago</div>
              <div style={{ fontSize: 12, opacity: 0.85, marginTop: 2 }}>09:42 · 4G connection</div>
            </div>
            <div style={{
              width: 56, height: 56, borderRadius: '50%',
              background: 'rgba(255,255,255,0.18)', color: c.gold,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name="cloud-up" size={26} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 14, marginTop: 16, paddingTop: 14,
                        borderTop: '1px solid rgba(255,255,255,0.18)' }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, opacity: 0.7 }}>Pending</div>
              <div style={{ fontSize: 18, fontWeight: 800, color: c.gold }}>4 records</div>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, opacity: 0.7 }}>Storage</div>
              <div style={{ fontSize: 18, fontWeight: 800 }}>3.4 / 50 MB</div>
            </div>
          </div>
        </Card>
      </div>

      <SectionHeader title="Pending uploads · 4" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={4}>
          {items.map((it, i, a) => (
            <React.Fragment key={i}>
              <Row icon={<Icon name={it.icon} size={20} />} title={it.t} subtitle={it.s} tone="primary"
                   right={<Pill size="sm" tone={it.state === 'pending' ? 'pending' : 'default'}>
                     {it.state === 'pending' ? 'Queued' : 'Waiting'}
                   </Pill>} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>
      </div>

      <SectionHeader title="Sync settings" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={4}>
          <ToggleRow icon={<Icon name="cloud-up" size={20} />} title="Auto-sync on Wi-Fi" defaultOn />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <ToggleRow icon={<Icon name="phone" size={20} />} title="Sync on mobile data" subtitle="May incur costs" defaultOn={false} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <ToggleRow icon={<Icon name="camera" size={20} />} title="Compress photos before upload" defaultOn />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="trash" size={20} />} title="Clear local cache" subtitle="Frees ~12 MB" tone="danger" />
        </Card>
      </div>
    </Screen>
  );
}

function ScreenAudit({ navigate }) {
  const { c } = useGL();
  const log = [
    { t: 'Issued permit PRM-9821', d: '12 May 16:24 · Mary Phiri', icon: 'permit', tone: 'primary' },
    { t: 'Submitted inspection INS-2025-0412', d: '12 May 09:42 · Mary Phiri', icon: 'leaf', tone: 'primary' },
    { t: 'Opened arbitration ARB-1182', d: '12 May 18:24', icon: 'gavel', tone: 'gold' },
    { t: 'Registered grower TBZ-04610', d: '11 May 11:18 · Loveness Banda', icon: 'profile', tone: 'success' },
    { t: 'Captured sale SAL-2034', d: '12 May 14:08 · Lilayi', icon: 'bale', tone: 'gold' },
    { t: 'Signed in', d: '12 May 06:48 · Samsung A24', icon: 'lock', tone: 'info' },
  ];
  return (
    <Screen padded={false}>
      <ScreenHeader title="Activity log" subtitle="Last 7 days" onBack={() => navigate('profile')} />
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Permits', 'Inspections', 'Sales', 'System']} value="All" onChange={() => {}} />
      </div>
      <div style={{ padding: '8px 20px' }}>
        <Card padding={16}>
          {log.map((e, i) => (
            <div key={i} style={{ display: 'flex', gap: 12, paddingBottom: i < log.length - 1 ? 14 : 0 }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                <div style={{
                  width: 32, height: 32, borderRadius: '50%',
                  background: c[`${e.tone}Soft`], color: c[e.tone],
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}><Icon name={e.icon} size={14} /></div>
                {i < log.length - 1 && <div style={{ width: 2, flex: 1, background: c.outline, marginTop: 4 }} />}
              </div>
              <div style={{ flex: 1, paddingBottom: 4 }}>
                <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{e.t}</div>
                <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{e.d}</div>
              </div>
            </div>
          ))}
        </Card>
      </div>
    </Screen>
  );
}

function ScreenSearch({ navigate }) {
  const { c } = useGL();
  const [q, setQ] = React.useState('');
  return (
    <Screen padded={false}>
      <div style={{ padding: '14px 20px 8px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <button onClick={() => navigate('home')} style={{
          width: 40, height: 40, borderRadius: '50%', background: c.surfaceAlt,
          border: 'none', cursor: 'pointer', color: c.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><Icon name="arrow-left" size={18} /></button>
        <div style={{ flex: 1 }}>
          <SearchBar placeholder="Search growers, permits, NRC, plate…" value={q} onChange={setQ} autoFocus />
        </div>
      </div>
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Growers', 'Permits', 'Inspections', 'Disputes']} value="All" onChange={() => {}} />
      </div>
      <SectionHeader title="Recent" style={{ margin: '6px 24px 0' }} action="Clear" />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={4}>
          {[
            { i: 'profile', t: 'Mary Phiri', s: 'Grower · TBZ-04412', tone: 'gold' },
            { i: 'permit', t: 'PRM-9821', s: 'Permit · active', tone: 'primary' },
            { i: 'plant', t: 'Burley · curing', s: 'Search query', tone: 'info' },
          ].map((r, i, a) => (
            <React.Fragment key={i}>
              <Row icon={<Icon name={r.i} size={20} />} title={r.t} subtitle={r.s} tone={r.tone}
                   right={<Icon name="arrow-up-left" size={14} style={{ color: c.textSubtle }} />} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>
      </div>
      <SectionHeader title="Quick filters" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '0 20px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {[
          { t: 'High-risk this week', i: 'warning', tone: 'gold' },
          { t: 'Permits expiring 7d', i: 'permit', tone: 'primary' },
          { t: 'Growers without GPS', i: 'gps', tone: 'danger' },
          { t: 'My pending disputes', i: 'gavel', tone: 'info' },
        ].map((f, i) => (
          <Card key={i} padding={14}>
            <div style={{ width: 36, height: 36, borderRadius: 12,
                          background: c[`${f.tone === 'gold' ? 'gold' : f.tone}Soft`],
                          color: c[f.tone === 'gold' ? 'goldDeep' : f.tone],
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name={f.i} size={18} />
            </div>
            <div style={{ fontSize: 13, fontWeight: 700, color: c.text, marginTop: 10 }}>{f.t}</div>
          </Card>
        ))}
      </div>
    </Screen>
  );
}

Object.assign(window, {
  ScreenProfile, ScreenSettings, ScreenSecurity,
  ScreenNotifications, ScreenSync, ScreenAudit, ScreenSearch,
});
