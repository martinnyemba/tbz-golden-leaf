// screens-missing.jsx
// New screens from June 2026 feature update:
// Forgot Password, Grower Updates Queue, Grower Correction,
// Corrections Inbox, Permit Correction, Group Permit Correction.

// ─────────────────────────────────────────────────────────────
// FORGOT PASSWORD
// ─────────────────────────────────────────────────────────────
function ScreenForgotPassword({ navigate }) {
  const { c } = useGL();
  const [stage, setStage] = React.useState('input'); // input | sent
  const [email, setEmail] = React.useState('');

  if (stage === 'sent') {
    return (
      <Screen padded={false} footer={<Button variant="ghost" onClick={() => navigate('login')}>Back to sign in</Button>}>
        <div style={{ padding: '48px 28px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
          <div style={{
            width: 88, height: 88, borderRadius: '50%', background: c.primarySoft, color: c.primary,
            display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20,
          }}>
            <Icon name="mail" size={44} />
          </div>
          <div style={{ fontSize: 22, fontWeight: 800, color: c.text }}>Check your email</div>
          <div style={{ fontSize: 14, color: c.textMuted, marginTop: 10, lineHeight: 1.55, maxWidth: 280 }}>
            If <strong style={{ color: c.text }}>{email}</strong> is registered, you'll receive a password reset link shortly.
          </div>
          <Banner tone="info" icon={<Icon name="info" size={18} />} style={{ marginTop: 24, textAlign: 'left', width: '100%' }}
                  title="Web portal only"
                  sub="Password resets are handled on the TBZ web portal. Open the link on your browser or another device." />
          <Button onClick={() => setStage('input')} variant="ghost" style={{ marginTop: 16 }}>
            Try a different email
          </Button>
        </div>
      </Screen>
    );
  }

  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <Button onClick={() => { if (email.includes('@')) setStage('sent'); }}
                icon={<Icon name="mail" size={18} />}>
          Send reset link
        </Button>
        <Button variant="ghost" onClick={() => navigate('login')}>Back to sign in</Button>
      </div>
    }>
      <ScreenHeader title="Reset password" onBack={() => navigate('login')} />
      <div style={{ padding: '12px 28px 24px' }}>
        <div style={{
          width: 64, height: 64, borderRadius: 20, background: c.primarySoft, color: c.primary,
          display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18,
        }}>
          <Icon name="lock" size={30} />
        </div>
        <div style={{ fontSize: 22, fontWeight: 800, color: c.text }}>Forgot your password?</div>
        <div style={{ fontSize: 14, color: c.textMuted, marginTop: 8, lineHeight: 1.5 }}>
          Enter your work email and we'll send a reset link through the TBZ web portal.
        </div>
        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input label="Work email" value={email} onChange={(e) => setEmail(e.target.value)}
                 placeholder="you@tbz.org.zm" icon={<Icon name="mail" size={20} />}
                 type="email" required />
          <Banner tone="warning" icon={<Icon name="info" size={16} />}
                  title="Reset via web portal"
                  sub="Password resets use the TBZ portal at your configured base URL — no API available on mobile." />
        </div>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// GROWER UPDATES QUEUE
// ─────────────────────────────────────────────────────────────
function ScreenGrowerUpdatesQueue({ navigate }) {
  const { c } = useGL();
  const [filter, setFilter] = React.useState('All');
  const items = [
    { grower: 'Mary Phiri', id: 'TBZ-2024-04412', fields: 'Phone, address', status: 'pending', t: '12 May 09:42', err: null },
    { grower: 'Charles Tembo', id: 'TBZ-2024-03128', fields: 'District, province', status: 'synced', t: '11 May 14:20', err: null },
    { grower: 'Loveness Banda', id: 'TBZ-2024-04610', fields: 'NRC, date of birth', status: 'failed', t: '10 May 08:15',
      err: 'NRC already exists on another registered grower' },
    { grower: 'Felix Sakala', id: 'TBZ-2024-04501', fields: 'Email, phone', status: 'pending', t: '10 May 16:30', err: null },
    { grower: 'Gladys Mwale', id: 'TBZ-2024-02981', fields: 'Address, town', status: 'needs_review', t: '08 May 11:10',
      err: 'Grower status changed by approver — please confirm edit before retrying' },
  ];
  const visible = filter === 'All' ? items : items.filter(i => i.status === filter.toLowerCase().replace(' ', '_'));
  const toneMap = { pending: 'pending', synced: 'synced', failed: 'failed', needs_review: 'review' };

  return (
    <Screen padded={false}>
      <ScreenHeader title="Grower update queue" subtitle="Pending edits waiting to sync"
                    onBack={() => navigate('registration')}
                    right={<Button variant="secondary" size="sm" full={false}
                                   icon={<Icon name="cloud-up" size={16} />}>Sync all</Button>} />
      <div style={{ padding: '8px 20px', display: 'flex', gap: 10 }}>
        <KpiTile label="Pending" value="2" tone="gold" />
        <KpiTile label="Failed" value="1" tone="danger" />
        <KpiTile label="Synced" value="1" tone="success" />
      </div>
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Pending', 'Failed', 'Needs review', 'Synced']} value={filter} onChange={setFilter} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {visible.map((it, i) => (
          <Card key={i} padding={14} accent={it.status === 'failed' || it.status === 'needs_review' ? 'gold' : undefined}>
            <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
              <Avatar name={it.grower} size={40} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{it.grower}</div>
                  <Pill size="sm" tone={toneMap[it.status] || 'default'}>
                    {it.status.replace('_', ' ').replace(/^\w/, s => s.toUpperCase())}
                  </Pill>
                </div>
                <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace', marginTop: 2 }}>{it.id}</div>
                <div style={{ fontSize: 12, color: c.textMuted, marginTop: 4 }}>Changed: {it.fields}</div>
                <div style={{ fontSize: 11, color: c.textSubtle, marginTop: 2 }}>{it.t}</div>
                {it.err && (
                  <Banner tone={it.status === 'needs_review' ? 'warning' : 'danger'}
                          title={it.err} style={{ marginTop: 10 }} />
                )}
                <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                  {(it.status === 'pending' || it.status === 'failed' || it.status === 'needs_review') && (
                    <Button size="sm" full={false} variant="outline"
                            icon={<Icon name="edit" size={14} />}
                            onClick={() => navigate('grower-details')}>Edit</Button>
                  )}
                  {it.status !== 'synced' && (
                    <Button size="sm" full={false} variant="secondary"
                            icon={<Icon name="sync" size={14} />}>Sync</Button>
                  )}
                  {it.status !== 'synced' && (
                    <Button size="sm" full={false} variant="ghost"
                            icon={<Icon name="trash" size={14} />} danger>Discard</Button>
                  )}
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
// GROWER RETURNED FOR CORRECTION
// ─────────────────────────────────────────────────────────────
function ScreenGrowerCorrection({ navigate }) {
  const { c } = useGL();
  const [saved, setSaved] = React.useState(false);

  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false} onClick={() => setSaved(true)}
                icon={<Icon name="check" size={16} />}>
          {saved ? 'Saved locally' : 'Save draft'}
        </Button>
        <Button onClick={() => navigate('grower-details')}
                icon={<Icon name="cloud-up" size={18} />}>
          Fix & Resubmit
        </Button>
      </div>
    }>
      <ScreenHeader title="Fix & resubmit" subtitle="Returned for correction"
                    onBack={() => navigate('grower-details')} />

      {/* Correction banner */}
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14} style={{ background: c.goldSoft, border: `1.5px solid ${c.gold}`, borderRadius: 16 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <div style={{ width: 36, height: 36, borderRadius: 10, background: c.gold, color: c.text,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <Icon name="warning" size={18} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 6 }}>
                <Pill tone="returned" size="sm">Returned for correction</Pill>
                <div style={{ fontSize: 11, color: c.textMuted }}>09 May 2026</div>
              </div>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Reviewer: D. Mwansa</div>
              <div style={{ fontSize: 13, color: c.text, marginTop: 4, lineHeight: 1.5 }}>
                "NRC number does not match the uploaded ID document. Please correct the NRC and re-upload the ID front photo."
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Grower context */}
      <div style={{ padding: '4px 20px' }}>
        <Card padding={12} style={{ background: c.surfaceAlt, border: 'none' }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Avatar name="Mary Phiri" size={40} />
            <div>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Mary Phiri</div>
              <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace' }}>TBZ-2024-04412</div>
            </div>
            <div style={{ marginLeft: 'auto' }}>
              <Pill tone="returned" size="sm">Draft correction</Pill>
            </div>
          </div>
        </Card>
      </div>

      <SectionHeader title="Correction fields" style={{ margin: '12px 24px 0' }} />
      <div style={{ padding: '4px 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Input label="NRC / Passport / PACRA" value="224018/61/1" required
               icon={<Icon name="badge" size={20} />}
               helper="Flagged by reviewer — verify against original document" error="Check NRC number" />
        <Input label="First name" value="Mary" required />
        <Input label="Last name" value="Phiri" required />
        <Select label="Gender" value="Female" options={['Female', 'Male', 'Other']} />
        <Input label="Date of birth" value="14 / 06 / 1986" icon={<Icon name="calendar" size={20} />} />
        <Input label="Phone" value="+260 977 421 089" icon={<Icon name="phone" size={20} />} />
      </div>

      <SectionHeader title="Re-upload ID documents" style={{ margin: '16px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Banner tone="warning" icon={<Icon name="camera" size={18} />}
                title="Reviewer flagged ID photo"
                sub="Re-photograph the NRC front clearly in good light." />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 12 }}>
          {[
            { l: 'NRC · Front', flagged: true },
            { l: 'NRC · Back', flagged: false },
          ].map((p, i) => (
            <Card key={i} padding={0} style={{ overflow: 'hidden',
                                               borderColor: p.flagged ? c.gold : c.outlineSoft,
                                               borderWidth: p.flagged ? 2 : 1 }}>
              <ImageSlot label={p.flagged ? 'retake required' : ''} height={100} rounded={0} />
              <div style={{ padding: '8px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: c.text }}>{p.l}</div>
                {p.flagged
                  ? <Pill size="sm" tone="warning" icon={<Icon name="warning" size={11} />}>Flagged</Pill>
                  : <Icon name="check-circle" size={18} style={{ color: c.success }} />
                }
              </div>
            </Card>
          ))}
        </div>
      </div>

      {saved && (
        <div style={{ padding: '12px 20px' }}>
          <Banner tone="success" icon={<Icon name="check-circle" size={18} />}
                  title="Correction saved locally"
                  sub="Tap 'Fix & Resubmit' when ready to send for review." />
        </div>
      )}
      <div style={{ height: 8 }} />
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// CORRECTIONS INBOX (cross-module)
// ─────────────────────────────────────────────────────────────
function ScreenCorrectionsInbox({ navigate }) {
  const { c } = useGL();
  const [filter, setFilter] = React.useState('All');
  const items = [
    { kind: 'grower', icon: 'profile', name: 'Mary Phiri', ref: 'TBZ-2024-04412',
      reason: 'NRC does not match ID document photo', returned: '09 May 2026',
      sync: 'pending', nav: 'grower-correction' },
    { kind: 'permit', icon: 'permit', name: 'Permit PRM-9812', ref: 'PRM-9812',
      reason: 'Vehicle plate mismatch — re-enter plate number', returned: '10 May 2026',
      sync: 'pending', nav: 'permit-correction' },
    { kind: 'permit', icon: 'permit', name: 'Permit PRM-9790', ref: 'PRM-9790',
      reason: 'Destination sales floor not available — select alternate', returned: '08 May 2026',
      sync: 'failed', nav: 'permit-correction' },
    { kind: 'group', icon: 'users', name: 'Group Permit GRP-4481', ref: 'GRP-4481',
      reason: 'Manifest has only 1 grower — minimum 2 required', returned: '07 May 2026',
      sync: 'pending', nav: 'group-permit-correction' },
  ];
  const kindMap = { grower: 'Growers', permit: 'Permits', group: 'Group permits' };
  const visible = filter === 'All' ? items
    : items.filter(i => i.kind === filter.toLowerCase().split(' ')[0]);

  return (
    <Screen padded={false}>
      <ScreenHeader title="Corrections inbox" subtitle={`${items.length} items returned`}
                    onBack={() => navigate('home')} />
      <div style={{ padding: '8px 20px', display: 'flex', gap: 10 }}>
        <KpiTile label="Growers" value="1" tone="gold" />
        <KpiTile label="Permits" value="2" tone="gold" />
        <KpiTile label="Group" value="1" tone="gold" />
      </div>
      <div style={{ padding: '4px 20px' }}>
        <Banner tone="warning" icon={<Icon name="warning" size={18} />}
                title="Action required"
                sub="Review the correction reason, fix the issues highlighted, and resubmit for approval." />
      </div>
      <div style={{ padding: '8px 20px' }}>
        <FilterPills options={['All', 'Growers', 'Permits', 'Group permits']} value={filter} onChange={setFilter} />
      </div>
      <div style={{ padding: '4px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {visible.map((it, i) => (
          <Card key={i} padding={14} accent="gold" onClick={() => navigate(it.nav)}>
            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{
                width: 44, height: 44, borderRadius: 14, flexShrink: 0,
                background: c.goldSoft, color: c.goldDeep,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={it.icon} size={22} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{it.name}</div>
                  <Pill size="sm" tone="returned">Returned</Pill>
                </div>
                <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace', marginTop: 2 }}>{it.ref}</div>
                <div style={{ fontSize: 12, color: c.text, marginTop: 6, lineHeight: 1.4,
                              background: c.goldSoft, padding: '6px 10px', borderRadius: 8 }}>
                  "{it.reason}"
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                  <div style={{ fontSize: 11, color: c.textMuted }}>Returned {it.returned}</div>
                  <SyncChip status={it.sync} />
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
      <div style={{ height: 24 }} />
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// PERMIT CORRECTION (transport permit returned for correction)
// ─────────────────────────────────────────────────────────────
function ScreenPermitCorrection({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false} icon={<Icon name="check" size={16} />}>Save draft</Button>
        <Button onClick={() => navigate('permit-detail')} icon={<Icon name="cloud-up" size={18} />}>
          Fix & Resubmit
        </Button>
      </div>
    }>
      <ScreenHeader title="Fix permit" subtitle="Returned for correction" onBack={() => navigate('permits')} />

      {/* Correction banner */}
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14} style={{ background: c.goldSoft, border: `1.5px solid ${c.gold}`, borderRadius: 16 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <div style={{ width: 36, height: 36, borderRadius: 10, background: c.gold, color: c.text,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <Icon name="warning" size={18} />
            </div>
            <div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 6 }}>
                <Pill tone="returned" size="sm">Returned — PRM-9812</Pill>
              </div>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Reviewer: D. Mwansa · 10 May 2026</div>
              <div style={{ fontSize: 13, color: c.text, marginTop: 4, lineHeight: 1.5 }}>
                "Vehicle plate ZM ABG 4421 does not match the fleet register. Please correct the plate number and resubmit."
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Read-only grower context */}
      <div style={{ padding: '4px 20px' }}>
        <Card padding={12} style={{ background: c.surfaceAlt, border: 'none' }}>
          <FieldRow label="Grower" value="Mary Phiri · TBZ-04412" />
          <FieldRow label="Bales · Weight" value="24 · 712 kg" />
          <FieldRow label="Tobacco type" value="Burley" />
        </Card>
      </div>

      <SectionHeader title="Transport details" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Input label="Vehicle licence plate" value="ZM ABG 4421"
               icon={<Icon name="truck" size={20} />}
               error="Does not match fleet register" required />
        <Input label="Driver name & ID" value="Joseph Phiri · DRV-2812" icon={<Icon name="profile" size={20} />} />
        <Input label="Total bales" value="24" />
        <Input label="Weight (kg)" value="712" />
        <Select label="Purpose" value="Sales" options={['Sales', 'Processing', 'Storage', 'Export']} />
        <Select label="Origin province" value="Eastern" options={['Eastern', 'Central', 'Southern', 'Northern', 'Lusaka']} />
        <Select label="Origin district" value="Chadiza" options={['Chadiza', 'Chipata', 'Katete', 'Lundazi', 'Petauke']} />
        <Select label="Destination sales floor" value="Lilayi sales floor" options={['Lilayi sales floor', 'Kabwe sales floor', 'Eastern Auction Floor']} />

        <SectionHeader title="Buyer & notes" />
        <Select label="Buyer" value="Universal Leaf · LIC-0142" options={['None', 'Universal Leaf · LIC-0142', 'Alliance One · LIC-0218']} />
        <Input label="Comments" multiline value="Re-submitted with correct plate number." />
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// GROUP PERMIT CORRECTION
// ─────────────────────────────────────────────────────────────
function ScreenGroupPermitCorrection({ navigate }) {
  const { c } = useGL();
  const [growers, setGrowers] = React.useState([
    { name: 'Mary Phiri', id: 'TBZ-04412', bales: 12, kg: 348 },
  ]);

  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false} icon={<Icon name="check" size={16} />}>Save draft</Button>
        <Button onClick={() => navigate('permits')} icon={<Icon name="cloud-up" size={18} />}>
          Fix & Resubmit
        </Button>
      </div>
    }>
      <ScreenHeader title="Fix group permit" subtitle="Returned for correction" onBack={() => navigate('permits')} />

      <div style={{ padding: '8px 20px' }}>
        <Card padding={14} style={{ background: c.goldSoft, border: `1.5px solid ${c.gold}`, borderRadius: 16 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <div style={{ width: 36, height: 36, borderRadius: 10, background: c.gold, color: c.text,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <Icon name="warning" size={18} />
            </div>
            <div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 6 }}>
                <Pill tone="returned" size="sm">Returned — GRP-4481</Pill>
              </div>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>07 May 2026</div>
              <div style={{ fontSize: 13, color: c.text, marginTop: 4, lineHeight: 1.5 }}>
                "Group permit manifest has only 1 grower — a minimum of 2 growers is required before the permit can be approved."
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Manifest — must have ≥ 2 */}
      <div style={{ padding: '8px 20px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', padding: '4px 4px 10px' }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: c.text }}>Growers · {growers.length} of 2+ required</div>
          <button style={{ background: 'none', border: 'none', color: c.primary, fontSize: 13, fontWeight: 700, cursor: 'pointer' }}>
            + Add grower
          </button>
        </div>

        {growers.length < 2 && (
          <Banner tone="danger" icon={<Icon name="warning" size={16} />}
                  title="Minimum 2 growers required"
                  sub="Add at least 1 more grower before resubmitting." style={{ marginBottom: 12 }} />
        )}

        <Card padding={4}>
          {growers.map((g, i) => (
            <React.Fragment key={i}>
              {i > 0 && <div style={{ height: 1, background: c.outlineSoft }} />}
              <Row icon={<Avatar name={g.name} size={32} />} title={g.name}
                   subtitle={`${g.id} · ${g.bales} bales · ${g.kg} kg`}
                   right={<button style={{ background: 'none', border: 'none', color: c.danger, cursor: 'pointer', padding: 4 }}>
                     <Icon name="trash" size={16} />
                   </button>} />
            </React.Fragment>
          ))}
        </Card>

        {/* Add grower search */}
        <Card padding={14} style={{ marginTop: 10, background: c.surfaceAlt, border: 'none' }}>
          <SearchBar placeholder="Search grower by name, TBZ ID…" />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 10 }}>
            {[
              { n: 'Charles Tembo', id: 'TBZ-03128' },
              { n: 'Felix Sakala', id: 'TBZ-04501' },
            ].map((g) => (
              <div key={g.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                       padding: '8px 0', borderBottom: `1px solid ${c.outlineSoft}` }}>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{g.n}</div>
                  <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace' }}>{g.id}</div>
                </div>
                <Button size="sm" full={false} icon={<Icon name="plus" size={14} />}
                        onClick={() => setGrowers([...growers, { name: g.n, id: g.id, bales: 0, kg: 0 }])}>
                  Add
                </Button>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <SectionHeader title="Vehicle & route" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px 0', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Input label="Vehicle licence plate" value="ZM CDG 7702" icon={<Icon name="truck" size={20} />} />
        <Select label="Origin province" value="Eastern" options={['Eastern', 'Central', 'Southern']} />
        <Select label="Origin district" value="Chipata" options={['Chipata', 'Chadiza', 'Katete']} />
        <Select label="Destination sales floor" value="Lilayi sales floor" options={['Lilayi sales floor', 'Kabwe sales floor']} />
        <Select label="Purpose" value="Sales" options={['Sales', 'Processing', 'Storage', 'Export']} />
        <Input label="Comments" multiline value="Added second grower as required. Vehicle plate confirmed." />
      </div>
    </Screen>
  );
}

Object.assign(window, {
  ScreenForgotPassword,
  ScreenGrowerUpdatesQueue,
  ScreenGrowerCorrection,
  ScreenCorrectionsInbox,
  ScreenPermitCorrection,
  ScreenGroupPermitCorrection,
});
