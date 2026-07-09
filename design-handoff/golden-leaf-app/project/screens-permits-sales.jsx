// screens-permits-sales.jsx
// Permits (list, detail, new request, validation/scan, group permit) and Sales/Marketing.

// ─────────────────────────────────────────────────────────────
// PERMIT LIST
// ─────────────────────────────────────────────────────────────
function ScreenPermits({ navigate }) {
  const { c } = useGL();
  const [filter, setFilter] = React.useState('All');
  const items = [
    { id: 'PRM-9821', grower: 'Mary Phiri', sub: '24 bales · 712 kg · Burley', valid: 'Valid · 12-19 May', status: 'active', tone: 'active' },
    { id: 'PRM-9820', grower: 'Charles Tembo', sub: '18 bales · 524 kg · Virginia', valid: 'Valid · 12-19 May', status: 'active', tone: 'active' },
    { id: 'PRM-9809', grower: 'Group · Eastern Coop', sub: '5 growers · 64 bales · 1.8t', valid: 'Pending approval', status: 'pending', tone: 'pending', group: true },
    { id: 'PRM-9742', grower: 'Mary Phiri', sub: '18 bales · 524 kg', valid: 'Used · 04 Apr', status: 'used', tone: 'draft' },
    { id: 'PRM-9701', grower: 'Felix Sakala', sub: '32 bales · 928 kg', valid: 'Used · 28 Mar', status: 'used', tone: 'draft' },
    { id: 'PRM-9612', grower: 'Beatrice Mtonga', sub: '14 bales · 412 kg', valid: 'Rejected · No NRC', status: 'rejected', tone: 'failed' },
  ];
  const visible = filter === 'All' ? items : items.filter(p => p.status === filter.toLowerCase());
  return (
    <Screen padded={false}>
      <ScreenHeader title="Permits" subtitle="32 issued this month" onBack={() => navigate('home')}
                    right={<button onClick={() => navigate('permit-validate')} style={{
                      background: c.gold, color: '#1A1308', border: 'none', borderRadius: 999,
                      padding: '8px 14px', fontSize: 12, fontWeight: 700, cursor: 'pointer',
                      display: 'inline-flex', alignItems: 'center', gap: 4,
                    }}><Icon name="qr" size={14} /> Scan</button>} />
      <div style={{ padding: '8px 20px' }}>
        <SearchBar placeholder="Search by permit ID, grower, NRC…" />
      </div>
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Active', 'Pending', 'Used', 'Rejected']} value={filter} onChange={setFilter} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', gap: 10 }}>
        <KpiTile label="Active" value="14" tone="primary" />
        <KpiTile label="Pending" value="3" tone="gold" />
        <KpiTile label="Bales" value="412" tone="default" />
      </div>
      <div style={{ padding: '12px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {visible.map((p) => (
          <Card key={p.id} padding={0} onClick={() => navigate('permit-detail')} style={{ overflow: 'hidden' }}>
            <div style={{ padding: 14, display: 'flex', gap: 14, alignItems: 'center' }}>
              <div style={{
                width: 56, height: 56, borderRadius: 14, flexShrink: 0,
                background: p.status === 'active' ? c.primarySoft : p.status === 'pending' ? c.goldSoft : c.surfaceAlt,
                color: p.status === 'active' ? c.primary : p.status === 'pending' ? c.goldDeep : c.textMuted,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={p.group ? 'users' : 'permit'} size={26} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, alignItems: 'flex-start' }}>
                  <div style={{ fontSize: 14, fontWeight: 800, color: c.text, fontFamily: 'ui-monospace, monospace' }}>{p.id}</div>
                  <Pill size="sm" tone={p.tone}>{p.status[0].toUpperCase() + p.status.slice(1)}</Pill>
                </div>
                <div style={{ fontSize: 13, fontWeight: 600, color: c.text, marginTop: 2 }}>{p.grower}</div>
                <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{p.sub}</div>
              </div>
            </div>
            <div style={{ padding: '8px 14px', borderTop: `1px solid ${c.outlineSoft}`, background: c.surfaceAlt,
                          display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ fontSize: 11, color: c.textMuted, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <Icon name="calendar" size={12} /> {p.valid}
              </div>
              <Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />
            </div>
          </Card>
        ))}
      </div>
      <FAB icon={<Icon name="plus" size={20} />} label="New" onClick={() => navigate('permit-new')} />
      <div style={{ height: 24 }} />
    </Screen>
  );
}

function ScreenPermitDetail({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 8 }}>
        <Button variant="gold" full={false} icon={<Icon name="warning" size={16} />}
                onClick={() => navigate('permit-correction')}>
          Fix &amp; Resubmit
        </Button>
        <Button variant="outline" full={false} icon={<Icon name="share" size={16} />}>Share</Button>
        <Button onClick={() => navigate('permit-validate')} icon={<Icon name="qr" size={16} />}>Validate</Button>
      </div>
    }>
      <ScreenHeader title="Permit detail" onBack={() => navigate('permits')} />
      <div style={{ padding: '8px 20px' }}>
        {/* Permit ticket card */}
        <div style={{
          background: `linear-gradient(135deg, ${c.primaryDeep} 0%, ${c.primary} 100%)`,
          color: '#fff', borderRadius: 22, padding: 20, position: 'relative', overflow: 'hidden',
          boxShadow: '0 14px 40px -20px rgba(0,0,0,0.4)',
        }}>
          <div style={{ position: 'absolute', right: -30, top: -30, opacity: 0.08, pointerEvents: 'none' }}>
            <TbzMark size={180} />
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div style={{ fontSize: 11, opacity: 0.7, fontWeight: 700, letterSpacing: 0.5, textTransform: 'uppercase' }}>Transport permit</div>
              <div style={{ fontSize: 22, fontWeight: 800, fontFamily: 'ui-monospace, monospace', marginTop: 6 }}>PRM-9821</div>
            </div>
            <Pill tone="gold" size="md">Active</Pill>
          </div>
          <div style={{ display: 'flex', gap: 18, marginTop: 18, alignItems: 'center' }}>
            <QRPlaceholder size={108} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 11, opacity: 0.7 }}>Grower</div>
              <div style={{ fontSize: 16, fontWeight: 800 }}>Mary Phiri</div>
              <div style={{ fontSize: 11, opacity: 0.7, fontFamily: 'ui-monospace, monospace' }}>TBZ-2024-04412</div>
              <div style={{ display: 'flex', gap: 14, marginTop: 12 }}>
                <div>
                  <div style={{ fontSize: 9, opacity: 0.7, textTransform: 'uppercase', letterSpacing: 0.4, fontWeight: 700 }}>Bales</div>
                  <div style={{ fontSize: 18, fontWeight: 800, lineHeight: 1.1 }}>24</div>
                </div>
                <div>
                  <div style={{ fontSize: 9, opacity: 0.7, textTransform: 'uppercase', letterSpacing: 0.4, fontWeight: 700 }}>Weight</div>
                  <div style={{ fontSize: 18, fontWeight: 800, lineHeight: 1.1 }}>712 kg</div>
                </div>
                <div>
                  <div style={{ fontSize: 9, opacity: 0.7, textTransform: 'uppercase', letterSpacing: 0.4, fontWeight: 700 }}>Type</div>
                  <div style={{ fontSize: 18, fontWeight: 800, lineHeight: 1.1 }}>Burley</div>
                </div>
              </div>
            </div>
          </div>
          <div style={{
            marginTop: 16, paddingTop: 14, borderTop: '1px dashed rgba(255,255,255,0.25)',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11,
          }}>
            <span><span style={{ opacity: 0.7 }}>Valid · </span><span style={{ fontWeight: 700 }}>12 May → 19 May 2025</span></span>
            <span style={{ opacity: 0.85, fontFamily: 'ui-monospace, monospace' }}>4-of-7 days</span>
          </div>
        </div>

        <SectionHeader title="Movement" style={{ marginTop: 18 }} />
        <Card padding={14}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <div style={{ width: 36, height: 36, borderRadius: 12, background: c.primarySoft, color: c.primary,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="map-pin" size={18} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12, color: c.textMuted }}>From</div>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Mphangwe, Chadiza</div>
            </div>
          </div>
          <div style={{ height: 16, marginLeft: 17, borderLeft: `2px dashed ${c.outline}` }} />
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <div style={{ width: 36, height: 36, borderRadius: 12, background: c.goldSoft, color: c.goldDeep,
                          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name="building" size={18} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12, color: c.textMuted }}>To</div>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Lilayi sales floor, Lusaka</div>
            </div>
          </div>
          <div style={{ height: 1, background: c.outlineSoft, margin: '14px 0' }} />
          <FieldRow label="Distance · ETA" value="612 km · ~9 h" />
          <FieldRow label="Vehicle" value="ZM ABG 4421 · Isuzu NPR" />
          <FieldRow label="Driver" value="Joseph Phiri · DRV-2812" />
        </Card>

        <SectionHeader title="Bales" style={{ marginTop: 18 }} action="View all" />
        <Card padding={4}>
          {[
            { n: 'B-001 → B-008', t: 'Burley · Lugs', kg: 234 },
            { n: 'B-009 → B-016', t: 'Burley · Cutters', kg: 248 },
            { n: 'B-017 → B-024', t: 'Burley · Tips', kg: 230 },
          ].map((b, i, a) => (
            <React.Fragment key={b.n}>
              <Row icon={<Icon name="bale" size={20} />} title={b.n} subtitle={b.t} tone="gold"
                   right={<Pill size="sm" tone="default">{b.kg} kg</Pill>} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>

        <SectionHeader title="Audit trail" style={{ marginTop: 18 }} />
        <Card padding={4}>
          <Row icon={<Icon name="check-circle" size={18} />} title="Approved" subtitle="11 May 16:24 · D. Mwansa" tone="primary" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="document" size={18} />} title="Submitted for review" subtitle="11 May 14:02 · J. Banda" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="edit" size={18} />} title="Permit drafted" subtitle="11 May 13:48 · J. Banda" />
        </Card>
      </div>
    </Screen>
  );
}

function ScreenPermitNew({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <Button onClick={() => navigate('permit-detail')} icon={<Icon name="check" size={18} />}>
        Submit for approval
      </Button>
    }>
      <ScreenHeader title="New permit" onBack={() => navigate('permits')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <SectionHeader title="Permit type" />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <Card padding={14} style={{ background: c.primarySoft, borderColor: c.primary, borderWidth: 2 }}>
            <Icon name="permit" size={24} style={{ color: c.primary }} />
            <div style={{ fontSize: 14, fontWeight: 700, color: c.text, marginTop: 8 }}>Single grower</div>
            <div style={{ fontSize: 11, color: c.textMuted }}>One registered grower</div>
          </Card>
          <Card padding={14} onClick={() => navigate('permit-group')}>
            <Icon name="users" size={24} style={{ color: c.textMuted }} />
            <div style={{ fontSize: 14, fontWeight: 700, color: c.text, marginTop: 8 }}>Group permit</div>
            <div style={{ fontSize: 11, color: c.textMuted }}>≥ 2 growers / cooperative</div>
          </Card>
        </div>

        <SectionHeader title="Grower" style={{ marginTop: 12 }} />
        <Card padding={14} accent="primary">
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Avatar name="Mary Phiri" size={44} gold />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Mary Phiri</div>
              <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace' }}>TBZ-2024-04412 · NRC 224018/61/1</div>
            </div>
            <button style={{ background: 'none', border: 'none', color: c.primary, fontSize: 12, fontWeight: 700, cursor: 'pointer' }}>Change</button>
          </div>
        </Card>

        <SectionHeader title="Consignment" />
        <div style={{ display: 'flex', gap: 10 }}>
          <Input label="Bales" value="24" />
          <Input label="Weight (kg)" value="712" />
        </div>
        <Select label="Tobacco type" value="Burley" options={['Burley', 'Virginia (Flue-cured)', 'Oriental', 'Dark fire-cured']} />
        <div style={{ display: 'flex', gap: 10 }}>
          <Input label="Valid from" value="12 May 2025" icon={<Icon name="calendar" size={20} />} />
          <Input label="Valid to" value="19 May 2025" icon={<Icon name="calendar" size={20} />} />
        </div>

        <SectionHeader title="Movement" />
        <Input label="Origin" value="Mphangwe, Chadiza" icon={<Icon name="map-pin" size={20} />} />
        <Select label="Destination" value="Lilayi sales floor" options={['Lilayi sales floor', 'Kabwe sales floor', 'Eastern Auction Floor', 'Other']} />
        <Input label="Vehicle plate" value="ZM ABG 4421" icon={<Icon name="truck" size={20} />} />
        <Input label="Driver name & ID" value="Joseph Phiri · DRV-2812" icon={<Icon name="profile" size={20} />} />

        <SectionHeader title="Compliance" />
        <Card padding={4}>
          {[
            { t: 'Active grower registration', v: true },
            { t: 'Last inspection passed (≤ 60 days)', v: true },
            { t: 'No open arbitration / disputes', v: false },
            { t: 'Bales tagged & accounted', v: true },
          ].map((k, i, a) => (
            <React.Fragment key={k.t}>
              <Row icon={<Icon name={k.v ? 'check-circle' : 'warning'} size={20} />} title={k.t}
                   tone={k.v ? 'primary' : 'gold'}
                   right={<Pill size="sm" tone={k.v ? 'success' : 'warning'}>{k.v ? 'OK' : 'Review'}</Pill>} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>
      </div>
    </Screen>
  );
}

function ScreenPermitGroup({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <Button onClick={() => navigate('permits')} icon={<Icon name="check" size={18} />}>Submit group permit</Button>
    }>
      <ScreenHeader title="Group permit" subtitle="Eastern Tobacco Coop" onBack={() => navigate('permit-new')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Banner tone="primary" icon={<Icon name="users" size={18} />} title="Cooperative consignment"
                sub="One permit covers multiple growers loading on the same vehicle." />

        <SectionHeader title="Growers · 5" action="Add grower" />
        <Card padding={4}>
          {[
            { name: 'Mary Phiri', id: 'TBZ-04412', bales: 12, kg: 348 },
            { name: 'Charles Tembo', id: 'TBZ-03128', bales: 16, kg: 472 },
            { name: 'Felix Sakala', id: 'TBZ-04501', bales: 18, kg: 524 },
            { name: 'Loveness Banda', id: 'TBZ-04610', bales: 8, kg: 228 },
            { name: 'Joseph Zulu', id: 'TBZ-09812', bales: 10, kg: 286 },
          ].map((g, i, a) => (
            <React.Fragment key={g.id}>
              <Row icon={<Avatar name={g.name} size={32} />} title={g.name}
                   subtitle={`${g.id} · ${g.bales} bales · ${g.kg} kg`}
                   right={<button style={{ background: 'none', border: 'none', color: c.danger, cursor: 'pointer' }}>
                     <Icon name="x" size={16} />
                   </button>} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>

        <Card padding={14} style={{ background: c.goldSoft, border: 'none' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: 11, fontWeight: 700, color: c.goldDeep, textTransform: 'uppercase', letterSpacing: 0.4 }}>Total consignment</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: c.goldDeep, marginTop: 4 }}>64 bales · 1 858 kg</div>
            </div>
            <Icon name="bale" size={36} style={{ color: c.goldDeep }} />
          </div>
        </Card>

        <Input label="Vehicle" value="ZM CDG 7702 · Hino 500" icon={<Icon name="truck" size={20} />} />
        <Input label="Driver" value="Patrick Kunda · DRV-3401" icon={<Icon name="profile" size={20} />} />
        <Select label="Destination" value="Lilayi sales floor" options={['Lilayi sales floor', 'Kabwe sales floor']} />
      </div>
    </Screen>
  );
}

function ScreenPermitValidate({ navigate }) {
  const { c } = useGL();
  const [stage, setStage] = React.useState('scan'); // scan / valid / rejected
  if (stage === 'scan') {
    return (
      <Screen padded={false}>
        <div style={{ flex: 1, background: '#0A0F0C', color: '#fff', display: 'flex', flexDirection: 'column' }}>
          <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <button onClick={() => navigate('permits')} style={{
              width: 40, height: 40, borderRadius: '50%', background: 'rgba(255,255,255,0.12)',
              border: 'none', color: '#fff', cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name="x" size={20} />
            </button>
            <div style={{ fontSize: 14, fontWeight: 700 }}>Validate permit</div>
            <button style={{
              width: 40, height: 40, borderRadius: '50%', background: 'rgba(255,255,255,0.12)',
              border: 'none', color: '#fff', cursor: 'pointer',
            }}><Icon name="flash" size={20} /></button>
          </div>
          <div style={{ flex: 1, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{
              width: 240, height: 240, borderRadius: 24,
              background: 'repeating-linear-gradient(135deg, #1B2C26 0 8px, #21342C 8px 16px)',
              position: 'relative', overflow: 'hidden',
            }}>
              <div style={{ position: 'absolute', inset: 16, border: '2px dashed rgba(255,255,255,0.4)', borderRadius: 14,
                            display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'rgba(255,255,255,0.6)' }}>
                <QRPlaceholder size={140} />
              </div>
              {[
                { t: 0, l: 0, br: 'tl' }, { t: 0, r: 0, br: 'tr' },
                { b: 0, l: 0, br: 'bl' }, { b: 0, r: 0, br: 'br' },
              ].map((p, i) => (
                <div key={i} style={{
                  position: 'absolute', top: p.t, left: p.l, right: p.r, bottom: p.b,
                  width: 36, height: 36,
                  borderTop: p.br[0] === 't' ? `3px solid ${c.gold}` : 'none',
                  borderBottom: p.br[0] === 'b' ? `3px solid ${c.gold}` : 'none',
                  borderLeft: p.br[1] === 'l' ? `3px solid ${c.gold}` : 'none',
                  borderRight: p.br[1] === 'r' ? `3px solid ${c.gold}` : 'none',
                  borderTopLeftRadius: p.br === 'tl' ? 14 : 0,
                  borderTopRightRadius: p.br === 'tr' ? 14 : 0,
                  borderBottomLeftRadius: p.br === 'bl' ? 14 : 0,
                  borderBottomRightRadius: p.br === 'br' ? 14 : 0,
                }} />
              ))}
              {/* scan line */}
              <div style={{
                position: 'absolute', left: 16, right: 16, top: '50%',
                height: 2, background: c.gold, boxShadow: `0 0 16px ${c.gold}`,
              }} />
            </div>
          </div>
          <div style={{ padding: '20px 28px 28px', textAlign: 'center' }}>
            <div style={{ fontSize: 14, fontWeight: 700 }}>Center the QR code in the frame</div>
            <div style={{ fontSize: 12, opacity: 0.7, marginTop: 6 }}>Permits also work offline — last sync 09:42</div>
            <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
              <Button variant="surface" onClick={() => setStage('valid')}
                      style={{ background: 'rgba(255,255,255,0.15)', borderColor: 'rgba(255,255,255,0.2)', color: '#fff' }}>
                Simulate valid scan
              </Button>
              <Button variant="surface" onClick={() => setStage('rejected')}
                      style={{ background: 'rgba(255,255,255,0.15)', borderColor: 'rgba(255,255,255,0.2)', color: '#fff' }}>
                Simulate reject
              </Button>
            </div>
          </div>
        </div>
      </Screen>
    );
  }
  const valid = stage === 'valid';
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="ghost" full={false} onClick={() => setStage('scan')}>Scan another</Button>
        <Button onClick={() => navigate(valid ? 'permit-detail' : 'permits')}>
          {valid ? 'Open permit' : 'Done'}
        </Button>
      </div>
    }>
      <div style={{ padding: '24px 20px 12px' }}>
        <Card padding={20} style={{
          background: valid
            ? `linear-gradient(180deg, ${c.successSoft} 0%, ${c.surface} 100%)`
            : `linear-gradient(180deg, ${c.dangerSoft} 0%, ${c.surface} 100%)`,
          border: `1px solid ${valid ? c.success : c.danger}30`, textAlign: 'center',
        }}>
          <div style={{
            width: 84, height: 84, borderRadius: '50%',
            background: valid ? c.success : c.danger, color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px',
          }}><Icon name={valid ? 'check' : 'x'} size={48} /></div>
          <div style={{ fontSize: 22, fontWeight: 800, color: c.text }}>
            {valid ? 'Permit is valid' : 'Permit rejected'}
          </div>
          <div style={{ fontSize: 13, color: c.textMuted, marginTop: 6 }}>
            {valid ? 'Cleared for movement to Lilayi sales floor' : 'Validation failed — see reasons below'}
          </div>
        </Card>
      </div>
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14}>
          <FieldRow label="Permit" value="PRM-9821" mono />
          <FieldRow label="Grower" value="Mary Phiri · TBZ-04412" />
          <FieldRow label="Bales · Weight" value="24 · 712 kg" />
          <FieldRow label="Type" value="Burley" />
          <FieldRow label="Validity" value="12 May → 19 May 2025" />
          <FieldRow label="Scanned at" value="Lilayi · 12 May 18:24" />
        </Card>
      </div>
      {!valid && (
        <div style={{ padding: '12px 20px' }}>
          <SectionHeader title="Rejection reasons" />
          <Card padding={4}>
            <Row icon={<Icon name="warning" size={20} />} title="Bale count mismatch"
                 subtitle="Permit lists 24 — 22 presented at floor" tone="danger" />
            <div style={{ height: 1, background: c.outlineSoft }} />
            <Row icon={<Icon name="document" size={20} />} title="Driver ID mismatch"
                 subtitle="DRV-2812 expected, DRV-3401 presented" tone="danger" />
          </Card>
          <Button variant="dangerOutline" style={{ marginTop: 14 }} onClick={() => navigate('arbitration-new')}>
            Open arbitration
          </Button>
        </div>
      )}
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// SALES / MARKETING
// ─────────────────────────────────────────────────────────────
function ScreenMarketing({ navigate }) {
  const { c } = useGL();
  const [tab, setTab] = React.useState('Pending');
  const pending = [
    { id: 'SAL-2034', floor: 'Lilayi · Floor A', grower: 'Mary Phiri', bales: 24, kg: 712, value: 11420, time: '12m ago' },
    { id: 'SAL-2031', floor: 'Lilayi · Floor B', grower: 'Charles Tembo', bales: 18, kg: 524, value: 8420, time: '1h ago' },
    { id: 'SAL-2029', floor: 'Kabwe', grower: 'Eastern Coop (5)', bales: 64, kg: 1858, value: 29140, time: '3h ago' },
  ];
  const settled = [
    { id: 'SAL-2018', floor: 'Lilayi', grower: 'Felix Sakala', bales: 32, kg: 928, value: 14860, time: 'Yesterday' },
    { id: 'SAL-2014', floor: 'Kabwe', grower: 'Mary Phiri', bales: 12, kg: 348, value: 5610, time: '2d ago' },
    { id: 'SAL-2009', floor: 'Eastern Auction', grower: 'Joseph Zulu', bales: 22, kg: 654, value: 10240, time: '4d ago' },
  ];
  const list = tab === 'Pending' ? pending : settled;
  return (
    <Screen padded={false}>
      <div style={{ background: `linear-gradient(180deg, ${c.primaryDeep} 0%, ${c.primary} 100%)`, color: '#fff' }}>
        <ScreenHeader title="Sales & marketing" subtitle="Season totals" sticky={false}
                      onBack={() => navigate('home')} />
        <div style={{ padding: '0 20px 24px' }}>
          <div style={{ display: 'flex', gap: 12, marginTop: 4 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, opacity: 0.75, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.4 }}>This season</div>
              <div style={{ fontSize: 28, fontWeight: 800, marginTop: 4, letterSpacing: -0.4 }}>K 412k</div>
              <div style={{ fontSize: 11, opacity: 0.7, marginTop: 2 }}>+18% vs last season</div>
            </div>
            <div style={{ flex: 1, paddingLeft: 14, borderLeft: '1px solid rgba(255,255,255,0.2)' }}>
              <div style={{ fontSize: 11, opacity: 0.75, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.4 }}>Bales sold</div>
              <div style={{ fontSize: 28, fontWeight: 800, marginTop: 4, letterSpacing: -0.4 }}>1 240</div>
              <div style={{ fontSize: 11, opacity: 0.7, marginTop: 2 }}>36 t · avg K 332/kg</div>
            </div>
          </div>
          {/* mini sparkline */}
          <svg width="100%" height="48" viewBox="0 0 320 48" style={{ marginTop: 14 }}>
            <defs>
              <linearGradient id="sparkFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={c.gold} stopOpacity="0.4" />
                <stop offset="100%" stopColor={c.gold} stopOpacity="0" />
              </linearGradient>
            </defs>
            <path d="M0 38 L20 32 L40 35 L60 26 L80 28 L100 22 L120 24 L140 14 L160 18 L180 12 L200 16 L220 8 L240 10 L260 6 L280 12 L300 4 L320 8 L320 48 L0 48 Z"
                  fill="url(#sparkFill)" />
            <path d="M0 38 L20 32 L40 35 L60 26 L80 28 L100 22 L120 24 L140 14 L160 18 L180 12 L200 16 L220 8 L240 10 L260 6 L280 12 L300 4 L320 8"
                  stroke={c.gold} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
      </div>
      <div style={{ padding: '0 20px', marginTop: -12 }}>
        <Card elevated padding={0}>
          <div style={{ display: 'flex' }}>
            {[
              { l: 'Floor today', v: 'Lilayi' },
              { l: 'Bales', v: '+126' },
              { l: 'Value', v: 'K 41.2k' },
            ].map((k, i, a) => (
              <div key={k.l} style={{ flex: 1, padding: '14px 8px', textAlign: 'center',
                                     borderRight: i < a.length - 1 ? `1px solid ${c.outlineSoft}` : 'none' }}>
                <div style={{ fontSize: 11, fontWeight: 700, color: c.textMuted, textTransform: 'uppercase', letterSpacing: 0.4 }}>{k.l}</div>
                <div style={{ fontSize: 14, fontWeight: 800, color: c.text, marginTop: 4 }}>{k.v}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>
      <div style={{ padding: '14px 20px 4px' }}>
        <FilterPills options={['Pending', 'Settled', 'Rejected']} value={tab} onChange={setTab} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {list.map((s) => (
          <Card key={s.id} padding={14} onClick={() => navigate('sale-detail')}
                accent={tab === 'Pending' ? 'gold' : undefined}>
            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{
                width: 44, height: 44, borderRadius: 12, flexShrink: 0,
                background: tab === 'Pending' ? c.goldSoft : c.successSoft,
                color: tab === 'Pending' ? c.goldDeep : c.success,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}><Icon name="bale" size={22} /></div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{s.grower}</div>
                  <Pill size="sm" tone={tab === 'Pending' ? 'pending' : 'success'}>
                    {tab === 'Pending' ? 'Awaiting capture' : 'Settled'}
                  </Pill>
                </div>
                <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>
                  {s.id} · {s.floor} · {s.time}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 }}>
                  <span style={{ fontSize: 12, color: c.textMuted }}>{s.bales} bales · {s.kg} kg</span>
                  <span style={{ fontSize: 16, fontWeight: 800, color: c.gold }}>K {s.value.toLocaleString()}</span>
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
      <FAB icon={<Icon name="plus" size={22} />} label="Capture" onClick={() => navigate('sale-capture')} />
      <div style={{ height: 24 }} />
    </Screen>
  );
}

function ScreenSaleDetail({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="dangerOutline" full={false} onClick={() => navigate('arbitration-new')}>Reject</Button>
        <Button onClick={() => navigate('marketing')} icon={<Icon name="check" size={18} />}>Settle sale</Button>
      </div>
    }>
      <ScreenHeader title="Sale capture" subtitle="SAL-2034" onBack={() => navigate('marketing')} />
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14} accent="gold">
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Avatar name="Mary Phiri" size={48} gold />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>Mary Phiri</div>
              <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace' }}>TBZ-04412 · PRM-9821</div>
            </div>
            <Pill tone="pending" size="sm">Pending</Pill>
          </div>
        </Card>
      </div>
      <SectionHeader title="Bale detail" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={4}>
          {[
            { tag: 'B-001', grade: 'Lugs · L1', kg: 28.4, k: 552, status: 'accepted' },
            { tag: 'B-002', grade: 'Lugs · L2', kg: 29.1, k: 538, status: 'accepted' },
            { tag: 'B-003', grade: 'Cutters · X1', kg: 30.2, k: 612, status: 'accepted' },
            { tag: 'B-004', grade: 'Cutters · X2', kg: 28.8, k: 0, status: 'rejected' },
            { tag: 'B-005', grade: 'Tips · T1', kg: 29.7, k: 624, status: 'accepted' },
          ].map((b, i, a) => (
            <React.Fragment key={b.tag}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 4px' }}>
                <div style={{ width: 36, height: 36, borderRadius: 10, background: c.surfaceAlt, color: c.text,
                              display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'ui-monospace, monospace',
                              fontSize: 11, fontWeight: 700 }}>{b.tag}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: c.text }}>{b.grade}</div>
                  <div style={{ fontSize: 11, color: c.textMuted, marginTop: 1 }}>{b.kg} kg</div>
                </div>
                {b.status === 'accepted' ? (
                  <div style={{ fontSize: 13, fontWeight: 800, color: c.text, fontFamily: 'ui-monospace, monospace' }}>K {b.k}</div>
                ) : (
                  <Pill size="sm" tone="danger">Rejected</Pill>
                )}
              </div>
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>
      </div>
      <SectionHeader title="Summary" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={14}>
          <FieldRow label="Floor" value="Lilayi · Floor A" />
          <FieldRow label="Buyer" value="Universal Leaf · LIC-0142" />
          <FieldRow label="Bales accepted" value="23 of 24" />
          <FieldRow label="Total weight" value="687.2 kg" />
          <FieldRow label="Avg price" value="K 16.62 / kg" />
          <div style={{ height: 1, background: c.outlineSoft, margin: '8px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: c.textMuted }}>Net to grower</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: c.gold }}>K 11 420</div>
          </div>
        </Card>
      </div>
    </Screen>
  );
}

function ScreenSaleCapture({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <Button onClick={() => navigate('sale-detail')}>Save sale</Button>
    }>
      <ScreenHeader title="Capture sale" onBack={() => navigate('marketing')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Card padding={14} onClick={() => navigate('permit-validate')} style={{ background: c.primarySoft, border: 'none' }}>
          <Row icon={<Icon name="qr" size={20} />} tone="primary"
               title="Scan permit QR" subtitle="Auto-fill grower and bale list"
               right={<Icon name="chevron-right" size={18} style={{ color: c.primary }} />} />
        </Card>
        <Select label="Sales floor" value="Lilayi · Floor A" options={['Lilayi · Floor A', 'Lilayi · Floor B', 'Kabwe', 'Eastern Auction']} required />
        <Select label="Buyer" value="Universal Leaf · LIC-0142" options={['Universal Leaf · LIC-0142', 'Alliance One · LIC-0218', 'JTI Tobacco · LIC-0301']} />
        <Input label="Permit reference" value="PRM-9821" icon={<Icon name="permit" size={20} />} />
        <SectionHeader title="Bales captured" action="Add" />
        <Card padding={14} style={{ background: c.surfaceAlt, border: 'none' }}>
          <div style={{ fontSize: 11, color: c.textMuted, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.4 }}>Captured this batch</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: c.text, marginTop: 4 }}>5 of 24 bales</div>
          <div style={{ height: 6, background: c.outline, borderRadius: 3, overflow: 'hidden', marginTop: 10 }}>
            <div style={{ width: '21%', height: '100%', background: c.primary }} />
          </div>
        </Card>
      </div>
    </Screen>
  );
}

// ─────────────────────────────────────────────────────────────
// ARBITRATION
// ─────────────────────────────────────────────────────────────
function ScreenArbitration({ navigate }) {
  const { c } = useGL();
  const items = [
    { id: 'ARB-1182', kind: 'Permit dispute', sub: 'Mary Phiri vs Lilayi sales floor', state: 'open', opened: '12 May', tone: 'gold' },
    { id: 'ARB-1175', kind: 'Bale rejection', sub: 'Charles Tembo · 4 bales', state: 'open', opened: '11 May', tone: 'gold' },
    { id: 'ARB-1162', kind: 'Movement breach', sub: 'Beatrice Mtonga · vehicle mismatch', state: 'review', opened: '08 May', tone: 'pending' },
    { id: 'ARB-1148', kind: 'Permit dispute', sub: 'Felix Sakala', state: 'closed', opened: '02 May', tone: 'success' },
  ];
  return (
    <Screen padded={false}>
      <ScreenHeader title="Arbitration" subtitle="Open disputes" onBack={() => navigate('home')} />
      <div style={{ padding: '8px 20px', display: 'flex', gap: 10 }}>
        <KpiTile label="Open" value="2" tone="gold" />
        <KpiTile label="In review" value="1" tone="info" />
        <KpiTile label="Closed (30d)" value="14" tone="success" />
      </div>
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Open', 'In review', 'Closed']} value="All" onChange={() => {}} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map((a) => (
          <Card key={a.id} padding={14} onClick={() => navigate('arbitration-detail')}
                accent={a.state === 'open' ? 'gold' : undefined}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div style={{ display: 'flex', gap: 12 }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 12,
                  background: c[`${a.tone === 'success' ? 'success' : a.tone === 'pending' ? 'warning' : 'gold'}Soft`],
                  color: a.tone === 'success' ? c.success : a.tone === 'pending' ? c.goldDeep : c.goldDeep,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                }}>
                  <Icon name="gavel" size={20} />
                </div>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 800, color: c.text, fontFamily: 'ui-monospace, monospace' }}>{a.id}</div>
                  <div style={{ fontSize: 13, color: c.text, fontWeight: 600, marginTop: 2 }}>{a.kind}</div>
                  <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{a.sub}</div>
                </div>
              </div>
              <Pill size="sm" tone={a.state === 'open' ? 'gold' : a.state === 'closed' ? 'success' : 'pending'}>
                {a.state[0].toUpperCase() + a.state.slice(1)}
              </Pill>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12, paddingTop: 10,
                          borderTop: `1px dashed ${c.outline}` }}>
              <span style={{ fontSize: 11, color: c.textMuted }}>Opened {a.opened}</span>
              <Icon name="chevron-right" size={16} style={{ color: c.textSubtle }} />
            </div>
          </Card>
        ))}
      </div>
      <FAB icon={<Icon name="plus" size={22} />} label="New" onClick={() => navigate('arbitration-new')} />
      <div style={{ height: 24 }} />
    </Screen>
  );
}

function ScreenArbitrationDetail({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false}>Add note</Button>
        <Button>Resolve dispute</Button>
      </div>
    }>
      <ScreenHeader title="Arbitration" subtitle="ARB-1182" onBack={() => navigate('arbitration')} />
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14} accent="gold">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
            <Pill tone="gold" size="sm" icon={<Icon name="gavel" size={11} />}>Open</Pill>
            <Pill tone="default" size="sm">Permit dispute</Pill>
          </div>
          <div style={{ fontSize: 16, fontWeight: 800, color: c.text }}>Bale count mismatch — PRM-9821</div>
          <div style={{ fontSize: 13, color: c.textMuted, marginTop: 6, lineHeight: 1.5 }}>
            Permit lists 24 bales for Mary Phiri; only 22 bales were presented at Lilayi Floor A on 12 May.
            Grower contests count and requests verification.
          </div>
        </Card>
      </div>
      <SectionHeader title="Parties" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={4}>
          <Row icon={<Avatar name="Mary Phiri" size={32} gold />} title="Mary Phiri"
               subtitle="Grower · TBZ-04412" tone="gold"
               right={<Pill size="sm" tone="warning">Claimant</Pill>} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="building" size={20} />} title="Lilayi Sales Floor A"
               subtitle="Operator · LIC-FLR-018" tone="info"
               right={<Pill size="sm" tone="info">Respondent</Pill>} />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Avatar name="Joseph Banda" size={32} />} title="Joseph Banda"
               subtitle="Inspector · Eastern" tone="primary"
               right={<Pill size="sm" tone="primary">Officer</Pill>} />
        </Card>
      </div>
      <SectionHeader title="Timeline" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={14}>
          {[
            { t: 'Dispute opened', d: '12 May 18:24 · J. Banda', icon: 'gavel', tone: 'gold' },
            { t: 'Floor reported 22 bales received', d: '12 May 18:18 · Lilayi', icon: 'document', tone: 'info' },
            { t: 'Permit issued for 24 bales', d: '11 May 16:24', icon: 'permit', tone: 'primary' },
          ].map((e, i, a) => (
            <div key={i} style={{ display: 'flex', gap: 12, paddingBottom: i < a.length - 1 ? 14 : 0 }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                <div style={{
                  width: 32, height: 32, borderRadius: '50%',
                  background: c[`${e.tone}Soft`], color: c[e.tone],
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}><Icon name={e.icon} size={14} /></div>
                {i < a.length - 1 && <div style={{ width: 2, flex: 1, background: c.outline, marginTop: 4 }} />}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>{e.t}</div>
                <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{e.d}</div>
              </div>
            </div>
          ))}
        </Card>
      </div>
      <SectionHeader title="Evidence · 3" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px 4px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
          <ImageSlot label="floor 1" height={88} rounded={14} />
          <ImageSlot label="floor 2" height={88} rounded={14} />
          <ImageSlot label="bales" height={88} rounded={14} />
        </div>
      </div>
    </Screen>
  );
}

function ScreenArbitrationNew({ navigate }) {
  const { c } = useGL();
  const [rejected, setRejected] = React.useState('Yes');
  return (
    <Screen padded={false} footer={
      <Button onClick={() => navigate('arbitration')} icon={<Icon name="check" size={18} />}>Submit arbitration</Button>
    }>
      <ScreenHeader title="Arbitration" subtitle="Scan bale & record outcome" onBack={() => navigate('arbitration')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Banner tone="warning" icon={<Icon name="gavel" size={18} />} title="Backend-verified"
                sub="Grower and grade details are resolved server-side from the bale ticket. Your signed-in identity is used as arbitrator." />

        {/* Bale ID + scanner */}
        <Card padding={14} onClick={() => {}} style={{ background: c.goldSoft, border: 'none', cursor: 'pointer' }}>
          <Row icon={<Icon name="qr" size={20} />} tone="gold"
               title="Scan bale barcode" subtitle="Tap to open scanner"
               right={<Icon name="chevron-right" size={18} style={{ color: c.goldDeep }} />} />
        </Card>
        <Input label="Bale ticket number" value="B-009" required
               icon={<Icon name="bale" size={20} />}
               helper="Scanned · resolved to Mary Phiri · Grade Cutters X1" />

        <Input label="Arbitration date" value="12 May 2025" required
               icon={<Icon name="calendar" size={20} />} />

        <SectionHeader title="Outcome" />
        <div style={{ display: 'flex', gap: 10 }}>
          {['Yes', 'No'].map((opt) => (
            <button key={opt} onClick={() => setRejected(opt)} style={{
              flex: 1, height: 52, borderRadius: 16,
              background: rejected === opt ? c.primary : c.surfaceAlt,
              color: rejected === opt ? '#fff' : c.text,
              border: `1.5px solid ${rejected === opt ? c.primary : 'transparent'}`,
              fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: 'inherit',
            }}>{opt === 'Yes' ? '✗ Rejected' : '✓ Accepted'}</button>
          ))}
        </div>

        {rejected === 'Yes' && (
          <Select label="Rejection reason" required value="HIGH_MOISTURE"
                  options={[
                    { value: 'NESTED', label: 'Nested' },
                    { value: 'HIGH_MOISTURE', label: 'High moisture' },
                    { value: 'LOW_MOISTURE', label: 'Low moisture' },
                    { value: 'NTRM', label: 'NTRM' },
                    { value: 'OVERWEIGHT', label: 'Overweight' },
                    { value: 'UNDERWEIGHT', label: 'Underweight' },
                    { value: 'NO_SALE', label: 'No sale' },
                  ]} />
        )}

        <Input label="Inspector remarks" multiline rows={3} placeholder="Optional notes on the arbitration outcome…" />
      </div>
    </Screen>
  );
}

Object.assign(window, {
  ScreenPermits, ScreenPermitDetail, ScreenPermitNew, ScreenPermitGroup,
  ScreenPermitValidate, ScreenMarketing, ScreenSaleDetail, ScreenSaleCapture,
  ScreenArbitration, ScreenArbitrationDetail, ScreenArbitrationNew,
});
