// screens-registration.jsx
// Grower registration list, multi-step new registration, grower details, edit, renewal.

// ─────────────────────────────────────────────────────────────
// REGISTRATION LIST
// ─────────────────────────────────────────────────────────────
function ScreenRegistration({ navigate }) {
  const { c } = useGL();
  const [filter, setFilter] = React.useState('All');
  const growers = [
    { id: 'TBZ-2024-04412', name: 'Mary Phiri', sub: 'Chadiza · 4.2 ha · Burley', status: 'active', risk: 'High', sync: 'pending', flagged: true },
    { id: 'TBZ-2024-03128', name: 'Charles Tembo', sub: 'Katete · 2.7 ha · Virginia', status: 'active', risk: 'Low', sync: 'synced' },
    { id: 'TBZ-2024-02981', name: 'Gladys Mwale', sub: 'Lundazi · 1.8 ha · Burley', status: 'review', risk: 'Med', sync: 'synced' },
    { id: 'TBZ-2024-04501', name: 'Felix Sakala', sub: 'Petauke · 6.4 ha · Virginia', status: 'active', risk: 'Low', sync: 'synced' },
    { id: 'TBZ-2024-04610', name: 'Loveness Banda', sub: 'Chipata · 0.9 ha · Burley', status: 'pending', risk: 'Med', sync: 'pending' },
    { id: 'TBZ-2023-09812', name: 'Joseph Zulu', sub: 'Mambwe · 3.1 ha · Burley', status: 'draft', risk: 'Low', sync: 'draft' },
  ];
  const visible = filter === 'All' ? growers : growers.filter(g => {
    if (filter === 'Active') return g.status === 'active';
    if (filter === 'Pending') return g.status === 'pending' || g.status === 'review';
    if (filter === 'High risk') return g.risk === 'High';
    if (filter === 'Drafts') return g.status === 'draft';
    return true;
  });
  return (
    <Screen padded={false}>
      <ScreenHeader title="Growers" subtitle="128 registered · 4 pending sync"
                    onBack={() => navigate('home')}
                    right={<button onClick={() => navigate('registration-new')} style={{
                      background: c.primary, color: '#fff', border: 'none', borderRadius: 999,
                      padding: '8px 14px', fontSize: 12, fontWeight: 700, cursor: 'pointer',
                      display: 'inline-flex', alignItems: 'center', gap: 4,
                    }}>
                      <Icon name="plus" size={16} /> New
                    </button>} />
      <div style={{ padding: '8px 20px' }}>
        <SearchBar placeholder="Search by name, NRC, ID…" onScan={() => navigate('scan-id')} />
      </div>
      <div style={{ padding: '4px 20px 4px' }}>
        <FilterPills options={['All', 'Active', 'Pending', 'High risk', 'Drafts']} value={filter} onChange={setFilter} />
      </div>
      <div style={{ padding: '8px 20px 0', display: 'flex', gap: 10 }}>
        <KpiTile label="Active" value="118" tone="primary" />
        <KpiTile label="Pending" value="6" tone="gold" />
        <KpiTile label="High risk" value="4" tone="danger" />
      </div>
      <div style={{ padding: '14px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {visible.map((g) => (
          <Card key={g.id} padding={14} accent={g.flagged ? 'gold' : undefined} onClick={() => navigate('grower-details')}>
            <div style={{ display: 'flex', gap: 12 }}>
              <Avatar name={g.name} size={44} gold={g.flagged} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, alignItems: 'flex-start' }}>
                  <div>
                    <div style={{ fontSize: 15, fontWeight: 700, color: c.text }}>{g.name}</div>
                    <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace', marginTop: 2 }}>{g.id}</div>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, alignItems: 'flex-end' }}>
                    <Pill size="sm" tone={g.status === 'active' ? 'active' : g.status === 'pending' ? 'pending' : g.status === 'draft' ? 'draft' : 'review'}>
                      {g.status[0].toUpperCase() + g.status.slice(1)}
                    </Pill>
                    {g.sync !== 'synced' && <SyncChip status={g.sync} />}
                  </div>
                </div>
                <div style={{ fontSize: 12, color: c.textMuted, marginTop: 6 }}>{g.sub}</div>
                <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
                  <Pill size="sm" tone={g.risk === 'High' ? 'danger' : g.risk === 'Med' ? 'warning' : 'success'}>
                    Risk · {g.risk}
                  </Pill>
                  {g.flagged && <Pill size="sm" tone="gold" icon={<Icon name="warning" size={11} />}>Flagged</Pill>}
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
      <FAB icon={<Icon name="plus" size={22} />} onClick={() => navigate('registration-new')} />
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// NEW REGISTRATION — STEP 1: TYPE
// ─────────────────────────────────────────────────────────────
function ScreenRegistrationNew({ navigate }) {
  const { c } = useGL();
  const [type, setType] = React.useState('small');
  const types = [
    { id: 'small', label: 'Small-scale', icon: 'profile', sub: 'Individual grower · ≤ 5 ha', count: '94' },
    { id: 'commercial', label: 'Commercial', icon: 'building', sub: '5–50 ha · Independent farm', count: '24' },
    { id: 'company', label: 'Company', icon: 'users', sub: 'Registered company / cooperative', count: '10' },
  ];
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="ghost" full={false} onClick={() => navigate('registration')}>Cancel</Button>
        <Button onClick={() => navigate('registration-new-2')} iconRight={<Icon name="arrow-right" size={18} />}>Continue</Button>
      </div>
    }>
      <ScreenHeader title="New grower" subtitle="1 of 4" onBack={() => navigate('registration')} />
      <Stepper step={1} total={4} labels={['Type', 'Identity', 'Farm', 'Photos']} />
      <div style={{ padding: '12px 20px' }}>
        <div style={{ fontSize: 18, fontWeight: 800, color: c.text }}>Grower type</div>
        <div style={{ fontSize: 13, color: c.textMuted, marginTop: 4 }}>This determines the fields required for registration.</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 18 }}>
          {types.map((t) => {
            const sel = type === t.id;
            return (
              <Card key={t.id} padding={14} onClick={() => setType(t.id)}
                    style={{ borderColor: sel ? c.primary : c.outlineSoft, borderWidth: sel ? 2 : 1, background: sel ? c.primarySoft : c.surface }}>
                <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
                  <div style={{
                    width: 44, height: 44, borderRadius: 14,
                    background: sel ? c.primary : c.surfaceAlt, color: sel ? '#fff' : c.textMuted,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}><Icon name={t.icon} size={22} /></div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: c.text }}>{t.label}</div>
                    <div style={{ fontSize: 12, color: c.textMuted, marginTop: 2 }}>{t.sub}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 18, fontWeight: 800, color: sel ? c.primary : c.text }}>{t.count}</div>
                    <div style={{ fontSize: 10, color: c.textMuted, fontWeight: 700, textTransform: 'uppercase' }}>in district</div>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
        <SectionHeader title="Quick capture" style={{ marginTop: 24 }} />
        <Card padding={14} onClick={() => navigate('scan-id')} style={{ background: c.goldSoft, border: 'none' }}>
          <Row icon={<Icon name="qr" size={20} />} title="Scan NRC card"
               subtitle="Auto-fill fields from a national ID"
               tone="gold" right={<Icon name="chevron-right" size={18} />} />
        </Card>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// NEW REGISTRATION — STEP 2: IDENTITY
// ─────────────────────────────────────────────────────────────
function ScreenRegistrationNew2({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="ghost" full={false} onClick={() => navigate('registration-new')}>Back</Button>
        <Button onClick={() => navigate('registration-new-3')} iconRight={<Icon name="arrow-right" size={18} />}>Continue</Button>
      </div>
    }>
      <ScreenHeader title="New grower" subtitle="2 of 4" onBack={() => navigate('registration-new')} />
      <Stepper step={2} total={4} labels={['Type', 'Identity', 'Farm', 'Photos']} />
      <div style={{ padding: '12px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Banner tone="primary" icon={<Icon name="info" size={18} />} title="NRC required"
                sub="National Registration Card is the primary identifier." />
        <Input label="Full name" placeholder="Mary Phiri" required value="Mary Phiri" success />
        <Input label="National Registration Card" placeholder="000000/00/0" required
               value="224018/61/1" icon={<Icon name="badge" size={20} />}
               suffix={<Pill tone="success" size="sm" icon={<Icon name="check" size={11} />}>Valid</Pill>}
               success />
        <div style={{ display: 'flex', gap: 10 }}>
          <Select label="Gender" placeholder="Select" value="Female" required options={['Female', 'Male', 'Other']} />
          <Input label="Date of birth" placeholder="DD / MM / YYYY" value="14 / 06 / 1986" icon={<Icon name="calendar" size={20} />} />
        </div>
        <Input label="Phone number" placeholder="+260" value="+260 977 421 089" icon={<Icon name="phone" size={20} />} />
        <Input label="Next of kin" placeholder="Name & relationship" value="John Phiri (husband)" />
        <Select label="Cooperative / association" placeholder="None" options={['None', 'Eastern Tobacco Farmers Coop', 'Chadiza Burley Group']} value="None" />
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// NEW REGISTRATION — STEP 3: FARM
// ─────────────────────────────────────────────────────────────
function ScreenRegistrationNew3({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="ghost" full={false} onClick={() => navigate('registration-new-2')}>Back</Button>
        <Button onClick={() => navigate('registration-new-4')} iconRight={<Icon name="arrow-right" size={18} />}>Continue</Button>
      </div>
    }>
      <ScreenHeader title="New grower" subtitle="3 of 4" onBack={() => navigate('registration-new-2')} />
      <Stepper step={3} total={4} labels={['Type', 'Identity', 'Farm', 'Photos']} />
      <div style={{ padding: '12px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', gap: 10 }}>
          <Select label="Province" placeholder="Select" value="Eastern" required options={['Eastern', 'Lusaka', 'Central', 'Southern', 'Northern']} />
          <Select label="District" placeholder="Select" value="Chadiza" required options={['Chadiza', 'Chipata', 'Katete', 'Lundazi', 'Petauke']} />
        </div>
        <Input label="Village / chief" placeholder="Village (Chief)" value="Mphangwe (Chief Mlolo)" />
        <Card padding={0} style={{ overflow: 'hidden' }}>
          <ImageSlot label="map · pinned 13.85°S 32.50°E" height={120} rounded={0} />
          <div style={{ padding: '12px 14px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>GPS location</div>
                <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace', marginTop: 2 }}>
                  -13.85044, 32.50217 · ±4 m
                </div>
              </div>
              <Button variant="secondary" size="sm" full={false} icon={<Icon name="gps" size={14} />}>Re-pin</Button>
            </div>
          </div>
        </Card>
        <div style={{ display: 'flex', gap: 10 }}>
          <Input label="Total area (ha)" value="4.2" suffix={<span style={{ fontSize: 12, fontWeight: 700, color: c.textMuted }}>ha</span>} />
          <Input label="Tobacco area (ha)" value="3.8" suffix={<span style={{ fontSize: 12, fontWeight: 700, color: c.textMuted }}>ha</span>} />
        </div>
        <Select label="Tobacco type" required value="Burley" options={['Burley', 'Virginia (Flue-cured)', 'Oriental', 'Dark fire-cured']} />
        <Select label="Curing structure" value="Open shed" options={['Open shed', 'Closed barn', 'Flue barn', 'Sun-cured frame']} />
        <Input label="Estimated yield (kg)" value="6 080" suffix={<span style={{ fontSize: 12, fontWeight: 700, color: c.textMuted }}>kg</span>} />
        <Input label="Notes" multiline placeholder="Additional notes…" value="Plot adjoins Mlolo stream — riparian buffer maintained at 12m." />
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// NEW REGISTRATION — STEP 4: PHOTOS
// ─────────────────────────────────────────────────────────────
function ScreenRegistrationNew4({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="ghost" full={false} onClick={() => navigate('registration-new-3')}>Back</Button>
        <Button onClick={() => navigate('registration-success')} icon={<Icon name="check" size={18} />}>Submit registration</Button>
      </div>
    }>
      <ScreenHeader title="New grower" subtitle="4 of 4" onBack={() => navigate('registration-new-3')} />
      <Stepper step={4} total={4} labels={['Type', 'Identity', 'Farm', 'Photos']} />
      <div style={{ padding: '12px 20px' }}>
        <Banner tone="warning" icon={<Icon name="camera" size={18} />} title="3 photos required"
                sub="NRC front · NRC back · Farm overview. Optional: signature, additional photos." />
        <SectionHeader title="Required" style={{ marginTop: 14 }} />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {[
            { label: 'NRC · Front', captured: true },
            { label: 'NRC · Back', captured: true },
            { label: 'Farm overview', captured: true },
            { label: 'Curing barn', captured: false },
          ].map((p, i) => (
            <Card key={i} padding={0} style={{ overflow: 'hidden', position: 'relative' }}>
              <ImageSlot label={p.captured ? '' : 'tap to capture'} height={120} rounded={0} />
              <div style={{ padding: '10px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: c.text }}>{p.label}</div>
                {p.captured ? <Icon name="check-circle" size={18} style={{ color: c.success }} /> : <Icon name="camera" size={18} style={{ color: c.textMuted }} />}
              </div>
            </Card>
          ))}
        </div>
        <SectionHeader title="Signature" style={{ marginTop: 16 }} />
        <Card padding={14}>
          <div style={{
            height: 100, borderRadius: 12, background: c.surfaceAlt, position: 'relative',
            display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
          }}>
            <svg width="240" height="60" viewBox="0 0 240 60" style={{ pointerEvents: 'none' }}>
              <path d="M10 40 C 30 10, 50 50, 70 30 S 110 50, 130 25 S 170 45, 200 32 L 220 38"
                    stroke={c.primary} strokeWidth="2.5" fill="none" strokeLinecap="round" />
            </svg>
            <div style={{ position: 'absolute', bottom: 8, left: 12, right: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ fontSize: 10, color: c.textMuted }}>Captured 09:42</div>
              <button style={{ background: 'none', border: 'none', color: c.primary, fontSize: 11, fontWeight: 700, cursor: 'pointer' }}>Re-sign</button>
            </div>
          </div>
        </Card>
        <SectionHeader title="Consent" style={{ marginTop: 16 }} />
        <Card padding={14}>
          <Row icon={<Icon name="check-circle" size={20} />} title="Grower has consented to data capture"
               subtitle="Per TBZ Privacy Policy v3" tone="primary"
               right={<Toggle value onChange={() => {}} />} />
        </Card>
      </div>
    </Screen>
  );
}

function ScreenRegistrationSuccess({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <Button onClick={() => navigate('grower-details')}>View grower</Button>
        <Button variant="ghost" onClick={() => navigate('registration')}>Back to growers list</Button>
      </div>
    }>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 40, textAlign: 'center' }}>
        <div style={{
          width: 96, height: 96, borderRadius: '50%', background: c.successSoft, color: c.success,
          display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16,
        }}>
          <Icon name="check" size={48} />
        </div>
        <div style={{ fontSize: 22, fontWeight: 800, color: c.text }}>Registered successfully</div>
        <div style={{ fontSize: 14, color: c.textMuted, marginTop: 8, maxWidth: 280 }}>
          Mary Phiri has been queued for sync. You'll receive a confirmation when the grower ID is issued.
        </div>
        <Card padding={14} style={{ marginTop: 24, width: '100%', maxWidth: 320 }}>
          <FieldRow label="Provisional ID" value="TBZ-2024-04412" mono />
          <FieldRow label="Status" value={<Pill tone="pending" size="sm">Pending sync</Pill>} />
          <FieldRow label="District" value="Chadiza" />
        </Card>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// GROWER DETAILS
// ─────────────────────────────────────────────────────────────
function ScreenGrowerDetails({ navigate }) {
  const { c } = useGL();
  const [tab, setTab] = React.useState('Overview');
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        {/* Show Fix & Resubmit prominently when RETURNED_FOR_CORRECTION */}
        <Button variant="gold" full={false} icon={<Icon name="warning" size={18} />}
                onClick={() => navigate('grower-correction')}>
          Fix &amp; Resubmit
        </Button>
        <Button variant="outline" full={false} icon={<Icon name="permit" size={18} />} onClick={() => navigate('permit-new')}>Permit</Button>
        <Button onClick={() => navigate('inspection-new')} icon={<Icon name="inspection" size={18} />}>Inspect</Button>
      </div>
    }>
      <div style={{ background: `linear-gradient(180deg, ${c.primaryDeep} 0%, ${c.primary} 100%)`, color: '#fff' }}>
        <ScreenHeader title="Grower" sticky={false}
                      onBack={() => navigate('registration')}
                      right={<button style={{ background: 'rgba(255,255,255,0.15)', border: 'none', color: '#fff', borderRadius: '50%', width: 36, height: 36, cursor: 'pointer' }}>
                        <Icon name="more" size={18} />
                      </button>} />
        <div style={{ padding: '4px 20px 28px', display: 'flex', gap: 14, alignItems: 'center' }}>
          <Avatar name="Mary Phiri" size={64} gold />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 20, fontWeight: 800 }}>Mary Phiri</div>
            <div style={{ fontSize: 12, opacity: 0.85, fontFamily: 'ui-monospace, monospace', marginTop: 2 }}>TBZ-2024-04412</div>
            <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
              <Pill tone="returned" size="sm" icon={<Icon name="warning" size={11} />}>Returned for correction</Pill>
              <Pill tone="outline" size="sm" style={{ color: '#fff', borderColor: 'rgba(255,255,255,0.3)', background: 'rgba(255,255,255,0.1)' }}>Active</Pill>
            </div>
          </div>
        </div>
      </div>
      <div style={{ padding: '0 20px', marginTop: -20 }}>
        <Card padding={0} elevated>
          <div style={{ display: 'flex' }}>
            {[
              { l: 'Area', v: '4.2', sub: 'ha' },
              { l: 'Permits', v: '7', sub: 'issued' },
              { l: 'Yield', v: '5.8t', sub: 'last yr' },
              { l: 'Risk', v: '78', sub: 'score', color: c.danger },
            ].map((k, i, a) => (
              <div key={k.l} style={{ flex: 1, padding: '14px 6px', textAlign: 'center', borderRight: i < a.length - 1 ? `1px solid ${c.outlineSoft}` : 'none' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: k.color || c.text, lineHeight: 1 }}>{k.v}</div>
                <div style={{ fontSize: 10, fontWeight: 700, color: c.textMuted, textTransform: 'uppercase', letterSpacing: 0.4, marginTop: 4 }}>{k.l}</div>
                <div style={{ fontSize: 10, color: c.textSubtle, marginTop: 1 }}>{k.sub}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>
      <div style={{ padding: '14px 20px 4px' }}>
        <FilterPills options={['Overview', 'Inspections', 'Permits', 'Sales', 'Documents']} value={tab} onChange={setTab} />
      </div>

      {tab === 'Overview' && (<>
        <div style={{ padding: '6px 20px' }}>
          <Card padding={14} accent="gold">
            <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
              <Icon name="warning" size={20} style={{ color: c.gold, flexShrink: 0, marginTop: 2 }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>High-risk indicators</div>
                <ul style={{ margin: '6px 0 0', padding: '0 0 0 16px', fontSize: 12, color: c.textMuted, lineHeight: 1.55 }}>
                  <li>Unverified curing barn capacity</li>
                  <li>Estimated yield exceeds field area benchmark</li>
                  <li>Open dispute · ARB-1182</li>
                </ul>
              </div>
            </div>
          </Card>
        </div>
        <SectionHeader title="Identity" style={{ margin: '8px 24px 0' }} />
        <div style={{ padding: '4px 20px' }}>
          <Card padding={14}>
            <FieldRow label="NRC" value="224018/61/1" mono />
            <FieldRow label="DOB / Gender" value="14 Jun 1986 · Female" />
            <FieldRow label="Phone" value="+260 977 421 089" />
            <FieldRow label="Cooperative" value="Eastern Tobacco Farmers Coop" />
            <FieldRow label="Registered" value="12 March 2024" />
          </Card>
        </div>
        <SectionHeader title="Farm" style={{ margin: '12px 24px 0' }} />
        <div style={{ padding: '4px 20px' }}>
          <Card padding={0} style={{ overflow: 'hidden' }}>
            <ImageSlot label="map · -13.850, 32.502" height={120} rounded={0} />
            <div style={{ padding: 14 }}>
              <FieldRow label="Province · District" value="Eastern · Chadiza" />
              <FieldRow label="Village · Chief" value="Mphangwe · Mlolo" />
              <FieldRow label="GPS" value="-13.85044, 32.50217" mono />
              <FieldRow label="Tobacco type" value="Burley" />
              <FieldRow label="Tobacco area" value="3.8 ha (4.2 total)" />
              <FieldRow label="Curing" value="Open shed · 1 unit" />
            </div>
          </Card>
        </div>
        <SectionHeader title="Recent activity" style={{ margin: '12px 24px 0' }} action="View all" />
        <div style={{ padding: '4px 20px 0' }}>
          <Card padding={4}>
            <Row icon={<Icon name="inspection" size={20} />} tone="primary"
                 title="Field inspection · passed"
                 subtitle="11 Apr 2025 · Inspector Banda"
                 right={<Pill tone="success" size="sm">Pass</Pill>} />
            <div style={{ height: 1, background: c.outlineSoft }} />
            <Row icon={<Icon name="permit" size={20} />} tone="gold"
                 title="Permit PRM-9742 issued"
                 subtitle="04 Apr 2025 · 18 bales · 524 kg"
                 right={<Pill tone="active" size="sm">Active</Pill>} />
            <div style={{ height: 1, background: c.outlineSoft }} />
            <Row icon={<Icon name="bale" size={20} />} tone="gold"
                 title="Sale captured at Lilayi floor"
                 subtitle="22 Mar 2025 · K8,420 · 18 bales" />
          </Card>
        </div>
      </>)}

      {tab === 'Inspections' && (
        <div style={{ padding: '6px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[
            { d: '11 Apr 2025', t: 'Field inspection', r: 'Pass', sub: '4 of 4 checks passed', tone: 'success' },
            { d: '20 Feb 2025', t: 'Nursery inspection', r: 'Pass', sub: '3 of 3 checks passed', tone: 'success' },
            { d: '18 Jan 2025', t: 'Pre-season visit', r: 'Review', sub: 'Curing barn capacity unverified', tone: 'pending' },
          ].map((it, i) => (
            <Card key={i} padding={14} onClick={() => navigate('inspection-detail')}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <Pill tone="primary" size="sm">{it.t}</Pill>
                <Pill tone={it.tone} size="sm">{it.r}</Pill>
              </div>
              <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{it.d}</div>
              <div style={{ fontSize: 12, color: c.textMuted, marginTop: 2 }}>{it.sub}</div>
            </Card>
          ))}
        </div>
      )}

      {tab === 'Permits' && (
        <div style={{ padding: '6px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[
            { id: 'PRM-9821', s: 'Active', tone: 'active', sub: '24 bales · 712 kg · valid 12-19 May' },
            { id: 'PRM-9742', s: 'Used', tone: 'draft', sub: '18 bales · 524 kg · 04 Apr 2025' },
            { id: 'PRM-9601', s: 'Used', tone: 'draft', sub: '12 bales · 348 kg · 22 Mar 2025' },
          ].map((p) => (
            <Card key={p.id} padding={14} onClick={() => navigate('permit-detail')}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div style={{ fontSize: 14, fontWeight: 800, color: c.text, fontFamily: 'ui-monospace, monospace' }}>{p.id}</div>
                <Pill size="sm" tone={p.tone}>{p.s}</Pill>
              </div>
              <div style={{ fontSize: 12, color: c.textMuted, marginTop: 4 }}>{p.sub}</div>
            </Card>
          ))}
        </div>
      )}

      {tab === 'Sales' && (
        <div style={{ padding: '6px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[
            { d: '22 Mar 2025', floor: 'Lilayi sales floor', bales: 18, kg: 524, k: 8420, tone: 'success' },
            { d: '04 Mar 2025', floor: 'Kabwe sales floor', bales: 12, kg: 348, k: 5610, tone: 'success' },
            { d: '14 Feb 2025', floor: 'Kabwe sales floor', bales: 6, kg: 174, k: 2740, tone: 'pending' },
          ].map((s, i) => (
            <Card key={i} padding={14}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{s.floor}</div>
                  <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{s.d} · {s.bales} bales · {s.kg} kg</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 14, fontWeight: 800, color: c.gold }}>K {s.k.toLocaleString()}</div>
                  <Pill size="sm" tone={s.tone}>{s.tone === 'success' ? 'Settled' : 'Pending'}</Pill>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {tab === 'Documents' && (
        <div style={{ padding: '6px 20px' }}>
          <Card padding={4}>
            {[
              { t: 'NRC · Front', d: '12 Mar 2024' },
              { t: 'NRC · Back', d: '12 Mar 2024' },
              { t: 'Farm overview photo', d: '12 Mar 2024' },
              { t: 'Curing barn photo', d: '11 Apr 2025' },
              { t: 'Signed consent form', d: '12 Mar 2024' },
            ].map((d, i, a) => (
              <React.Fragment key={d.t}>
                <Row icon={<Icon name="image" size={20} />} title={d.t} subtitle={d.d} tone="primary"
                     right={<Icon name="chevron-right" size={18} style={{ color: c.textSubtle }} />} />
                {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
              </React.Fragment>
            ))}
          </Card>
        </div>
      )}

      <div style={{ height: 24 }} />
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// SCAN ID
// ─────────────────────────────────────────────────────────────
function ScreenScanId({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false}>
      <div style={{ flex: 1, background: '#0A0F0C', color: '#fff', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <button onClick={() => navigate('registration-new')} style={{
            width: 40, height: 40, borderRadius: '50%', background: 'rgba(255,255,255,0.12)',
            border: 'none', color: '#fff', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="x" size={20} />
          </button>
          <div style={{ fontSize: 14, fontWeight: 700 }}>Scan NRC</div>
          <button style={{
            width: 40, height: 40, borderRadius: '50%', background: 'rgba(255,255,255,0.12)',
            border: 'none', color: '#fff', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><Icon name="flash" size={20} /></button>
        </div>
        <div style={{ flex: 1, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {/* viewfinder */}
          <div style={{
            width: 280, height: 180, borderRadius: 18,
            background: 'repeating-linear-gradient(135deg, #1B2C26 0 8px, #21342C 8px 16px)',
            position: 'relative', overflow: 'hidden',
          }}>
            <div style={{ position: 'absolute', inset: 16, border: '2px dashed rgba(255,255,255,0.4)', borderRadius: 12,
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: 11, color: 'rgba(255,255,255,0.6)', fontFamily: 'ui-monospace, monospace' }}>
              align NRC inside frame
            </div>
            {/* corners */}
            {[
              { t: 0, l: 0, br: 'tl' }, { t: 0, r: 0, br: 'tr' },
              { b: 0, l: 0, br: 'bl' }, { b: 0, r: 0, br: 'br' },
            ].map((p, i) => (
              <div key={i} style={{
                position: 'absolute', top: p.t, left: p.l, right: p.r, bottom: p.b,
                width: 32, height: 32,
                borderTop: p.br[0] === 't' ? `3px solid ${c.gold}` : 'none',
                borderBottom: p.br[0] === 'b' ? `3px solid ${c.gold}` : 'none',
                borderLeft: p.br[1] === 'l' ? `3px solid ${c.gold}` : 'none',
                borderRight: p.br[1] === 'r' ? `3px solid ${c.gold}` : 'none',
                borderTopLeftRadius: p.br === 'tl' ? 12 : 0,
                borderTopRightRadius: p.br === 'tr' ? 12 : 0,
                borderBottomLeftRadius: p.br === 'bl' ? 12 : 0,
                borderBottomRightRadius: p.br === 'br' ? 12 : 0,
              }} />
            ))}
            {/* fake ID readout */}
            <div style={{ position: 'absolute', bottom: 18, left: 18, fontSize: 9, color: 'rgba(255,255,255,0.5)', fontFamily: 'ui-monospace, monospace' }}>
              224018/61/1
            </div>
          </div>
        </div>
        <div style={{ padding: '20px 28px 28px' }}>
          <div style={{ background: 'rgba(255,255,255,0.08)', borderRadius: 16, padding: 14, marginBottom: 14 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: c.gold }}>Detected · 224018/61/1</div>
            <div style={{ fontSize: 14, fontWeight: 700, marginTop: 4 }}>Mary Phiri · F · 14 Jun 1986</div>
            <div style={{ fontSize: 11, opacity: 0.7, marginTop: 2 }}>Confidence 96% · Eastern Province</div>
          </div>
          <Button onClick={() => navigate('registration-new-2')}>Use this ID</Button>
          <Button variant="ghost" style={{ marginTop: 6, color: '#fff' }} onClick={() => navigate('registration-new-2')}>Enter manually</Button>
        </div>
      </div>
    </Screen>
  );
}

Object.assign(window, {
  ScreenRegistration, ScreenRegistrationNew, ScreenRegistrationNew2,
  ScreenRegistrationNew3, ScreenRegistrationNew4, ScreenRegistrationSuccess,
  ScreenGrowerDetails, ScreenScanId,
});
