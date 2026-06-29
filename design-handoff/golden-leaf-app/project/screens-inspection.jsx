// screens-inspection.jsx
// Inspection list, schedule, new inspection wizard (nursery/field/curing), detail, results, high-risk.

function ScreenInspectionList({ navigate }) {
  const { c } = useGL();
  const [filter, setFilter] = React.useState('Today');
  const items = [
    { d: 'Today', items: [
      { time: '09:30', name: 'Mary Phiri', id: 'TBZ-2024-04412', kind: 'Field', risk: 'High', loc: 'Chadiza · 4.8 km', tone: 'danger' },
      { time: '13:00', name: 'Charles Tembo', id: 'TBZ-2024-03128', kind: 'Curing', risk: 'Low', loc: 'Katete · 22 km', tone: 'info' },
      { time: '15:30', name: 'Felix Sakala', id: 'TBZ-2024-04501', kind: 'Field', risk: 'Low', loc: 'Petauke · 47 km', tone: 'info' },
    ]},
    { d: 'Tomorrow', items: [
      { time: '08:00', name: 'Gladys Mwale', id: 'TBZ-2024-02981', kind: 'Nursery', risk: 'Med', loc: 'Lundazi · 64 km', tone: 'warning' },
      { time: '11:00', name: 'Loveness Banda', id: 'TBZ-2024-04610', kind: 'Field', risk: 'Med', loc: 'Chipata · 8 km', tone: 'warning' },
    ]},
    { d: 'Thu, 14 May', items: [
      { time: '09:00', name: 'Joseph Zulu', id: 'TBZ-2023-09812', kind: 'Validation', risk: 'Low', loc: 'Mambwe · 31 km', tone: 'info' },
    ]},
  ];
  return (
    <Screen padded={false}>
      <ScreenHeader title="Inspections" subtitle="14 due this week" onBack={() => navigate('home')}
                    right={<button onClick={() => navigate('inspection-schedule')} style={{
                      background: c.surfaceAlt, border: 'none', borderRadius: 999, padding: '8px 12px',
                      fontSize: 12, fontWeight: 700, cursor: 'pointer', color: c.text,
                      display: 'inline-flex', alignItems: 'center', gap: 4,
                    }}><Icon name="calendar" size={14} /> Schedule</button>} />
      <div style={{ padding: '8px 20px', display: 'flex', gap: 10 }}>
        <KpiTile label="Today" value="3" tone="primary" icon={<Icon name="calendar" size={16} />} />
        <KpiTile label="High risk" value="2" tone="danger" icon={<Icon name="warning" size={16} />} />
        <KpiTile label="Pending sync" value="2" tone="gold" icon={<Icon name="cloud-up" size={16} />} />
      </div>
      <div style={{ padding: '8px 20px' }}>
        <FilterPills options={['Today', 'This week', 'Field', 'Nursery', 'Curing', 'Validation']} value={filter} onChange={setFilter} />
      </div>
      {items.map((g) => (
        <div key={g.d} style={{ padding: '10px 20px 0' }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: c.textMuted, textTransform: 'uppercase', letterSpacing: 0.5, padding: '4px 4px 8px' }}>{g.d}</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {g.items.map((it, i) => (
              <Card key={i} padding={14} accent={it.risk === 'High' ? 'gold' : undefined} onClick={() => navigate('inspection-detail')}>
                <div style={{ display: 'flex', gap: 12 }}>
                  <div style={{
                    width: 56, padding: '8px 0', textAlign: 'center', borderRadius: 12,
                    background: c.surfaceAlt, color: c.text, flexShrink: 0,
                  }}>
                    <div style={{ fontSize: 14, fontWeight: 800 }}>{it.time.split(':')[0]}</div>
                    <div style={{ fontSize: 10, color: c.textMuted, fontWeight: 700 }}>:{it.time.split(':')[1]}</div>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                      <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{it.name}</div>
                      <Pill tone={it.tone} size="sm">{it.kind}</Pill>
                    </div>
                    <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2, fontFamily: 'ui-monospace, monospace' }}>{it.id}</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                      <span style={{ fontSize: 12, color: c.textMuted, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                        <Icon name="map-pin" size={12} /> {it.loc}
                      </span>
                      <Pill size="sm" tone={it.risk === 'High' ? 'danger' : it.risk === 'Med' ? 'warning' : 'success'}>Risk · {it.risk}</Pill>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      ))}
      <FAB icon={<Icon name="plus" size={20} />} label="New" onClick={() => navigate('inspection-new')} />
      <div style={{ height: 24 }} />
    </Screen>
  );
}

function ScreenInspectionSchedule({ navigate }) {
  const { c } = useGL();
  const days = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
  const dates = [12, 13, 14, 15, 16, 17, 18];
  const counts = [3, 2, 1, 2, 4, 0, 0];
  const [sel, setSel] = React.useState(0);
  return (
    <Screen padded={false}>
      <ScreenHeader title="Schedule" subtitle="Week of 12 May" onBack={() => navigate('inspection')}
                    right={<button style={{ background: c.surfaceAlt, border: 'none', borderRadius: '50%', width: 36, height: 36, cursor: 'pointer' }}>
                      <Icon name="filter" size={16} />
                    </button>} />
      <div style={{ padding: '8px 20px' }}>
        <Card padding={12}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <div style={{ fontSize: 14, fontWeight: 800, color: c.text }}>May 2025</div>
            <div style={{ display: 'flex', gap: 4 }}>
              <button style={{ width: 28, height: 28, borderRadius: 8, border: 'none', background: c.surfaceAlt, color: c.text, cursor: 'pointer' }}>
                <Icon name="chevron-left" size={14} />
              </button>
              <button style={{ width: 28, height: 28, borderRadius: 8, border: 'none', background: c.surfaceAlt, color: c.text, cursor: 'pointer' }}>
                <Icon name="chevron-right" size={14} />
              </button>
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 6 }}>
            {days.map((d, i) => (
              <div key={i} style={{ textAlign: 'center', fontSize: 10, fontWeight: 700, color: c.textSubtle, padding: '4px 0' }}>{d}</div>
            ))}
            {dates.map((dt, i) => (
              <button key={i} onClick={() => setSel(i)} style={{
                aspectRatio: '1', border: 'none', borderRadius: 12,
                background: sel === i ? c.primary : 'transparent', color: sel === i ? '#fff' : c.text,
                cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center',
                justifyContent: 'center', gap: 2, fontFamily: 'inherit',
              }}>
                <span style={{ fontSize: 14, fontWeight: 700 }}>{dt}</span>
                {counts[i] > 0 && (
                  <span style={{ width: 5, height: 5, borderRadius: '50%', background: sel === i ? '#fff' : c.gold }} />
                )}
              </button>
            ))}
          </div>
        </Card>
      </div>
      <SectionHeader title={`${counts[sel]} inspections · 12 May`} style={{ margin: '12px 24px 0' }} />
      <div style={{ padding: '4px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {[
          { time: '09:30', name: 'Mary Phiri', kind: 'Field', dur: '90 min', tone: 'danger' },
          { time: '13:00', name: 'Charles Tembo', kind: 'Curing', dur: '60 min', tone: 'info' },
          { time: '15:30', name: 'Felix Sakala', kind: 'Field', dur: '90 min', tone: 'info' },
        ].map((it, i) => (
          <Card key={i} padding={14} onClick={() => navigate('inspection-detail')}>
            <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
              <div style={{ fontSize: 16, fontWeight: 800, color: c.primary, fontFamily: 'ui-monospace, monospace', width: 56 }}>{it.time}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{it.name}</div>
                <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                  <Pill size="sm" tone={it.tone}>{it.kind}</Pill>
                  <Pill size="sm" tone="default">{it.dur}</Pill>
                </div>
              </div>
              <Icon name="chevron-right" size={18} style={{ color: c.textSubtle }} />
            </div>
          </Card>
        ))}
      </div>
    </Screen>
  );
}

function ScreenInspectionNew({ navigate }) {
  const { c } = useGL();
  const [kind, setKind] = React.useState('field');
  const kinds = [
    { id: 'nursery', t: 'Nursery', s: 'Seedbed quality, pest scout', icon: 'plant' },
    { id: 'field', t: 'Field', s: 'Crop health, area, agronomy', icon: 'leaf' },
    { id: 'curing', t: 'Curing', s: 'Barns, leaf colour & moisture', icon: 'barn' },
    { id: 'validation', t: 'Validation', s: 'Pre-marketing audit', icon: 'shield-check' },
  ];
  return (
    <Screen padded={false} footer={
      <Button onClick={() => navigate('inspection-form')} iconRight={<Icon name="arrow-right" size={18} />}>
        Continue
      </Button>
    }>
      <ScreenHeader title="New inspection" onBack={() => navigate('inspection')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Banner tone="primary" icon={<Icon name="info" size={18} />} title="Linked to grower"
                sub="Mary Phiri · TBZ-2024-04412 · Chadiza" />
        <SectionHeader title="Inspection type" />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {kinds.map((k) => {
            const sel = kind === k.id;
            return (
              <Card key={k.id} padding={14} onClick={() => setKind(k.id)}
                    style={{ background: sel ? c.primarySoft : c.surface, borderColor: sel ? c.primary : c.outlineSoft, borderWidth: sel ? 2 : 1 }}>
                <div style={{ width: 36, height: 36, borderRadius: 12, background: sel ? c.primary : c.surfaceAlt,
                              color: sel ? '#fff' : c.textMuted, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Icon name={k.icon} size={18} />
                </div>
                <div style={{ fontSize: 14, fontWeight: 700, color: c.text, marginTop: 10 }}>{k.t}</div>
                <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{k.s}</div>
              </Card>
            );
          })}
        </div>
        <SectionHeader title="Schedule" style={{ marginTop: 12 }} />
        <div style={{ display: 'flex', gap: 10 }}>
          <Input label="Date" value="12 May 2025" icon={<Icon name="calendar" size={20} />} />
          <Input label="Time" value="09:30" icon={<Icon name="clock" size={20} />} />
        </div>
        <Select label="Inspector" value="Joseph Banda" options={['Joseph Banda', 'Esther Nyirenda', 'Peter Lungu']} />
        <Input label="Notes" multiline placeholder="Optional notes…" value="Follow up on curing barn capacity from previous visit." />
      </div>
    </Screen>
  );
}

function ScreenInspectionForm({ navigate }) {
  const { c } = useGL();
  const [checks, setChecks] = React.useState({
    'Plant population within range': true,
    'Inter-row spacing correct': true,
    'No tobacco mosaic visible': false,
    'Pest scouting documented': true,
    'Buffer to water source ≥ 10m': true,
    'No unregistered varieties': true,
  });
  const passed = Object.values(checks).filter(Boolean).length;
  const total = Object.keys(checks).length;
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false} onClick={() => navigate('inspection-detail')}>Save draft</Button>
        <Button onClick={() => navigate('inspection-result')}>Submit report</Button>
      </div>
    }>
      <ScreenHeader title="Field inspection" subtitle="Mary Phiri · TBZ-04412" onBack={() => navigate('inspection-new')} />
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <Card padding={14}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>Checklist progress</div>
            <Pill size="sm" tone={passed === total ? 'success' : 'pending'}>{passed} / {total}</Pill>
          </div>
          <div style={{ height: 6, background: c.surfaceAlt, borderRadius: 3, overflow: 'hidden' }}>
            <div style={{ width: `${(passed / total) * 100}%`, height: '100%', background: passed === total ? c.success : c.gold }} />
          </div>
        </Card>

        <SectionHeader title="Field measurements" />
        <Card padding={14}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <Input label="Measured area (ha)" value="3.85" />
            <Input label="Plant pop. /ha" value="14 200" />
            <Input label="Avg height (cm)" value="62" />
            <Input label="Healthy plants %" value="94" />
          </div>
        </Card>

        <SectionHeader title="Compliance checks" />
        <Card padding={4}>
          {Object.entries(checks).map(([k, v], i, a) => (
            <React.Fragment key={k}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 4px' }}>
                <button onClick={() => setChecks({ ...checks, [k]: !v })} style={{
                  width: 24, height: 24, borderRadius: 8, border: `1.5px solid ${v ? c.primary : c.outline}`,
                  background: v ? c.primary : 'transparent', cursor: 'pointer', flexShrink: 0,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff',
                }}>{v && <Icon name="check" size={14} />}</button>
                <div style={{ flex: 1, fontSize: 13, fontWeight: 600, color: v ? c.text : c.textMuted, lineHeight: 1.4 }}>{k}</div>
                {!v && <Pill size="sm" tone="danger">Failed</Pill>}
              </div>
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>

        <SectionHeader title="Photos & evidence" />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
          <ImageSlot label="overview" height={92} rounded={14} />
          <ImageSlot label="row 1" height={92} rounded={14} />
          <ImageSlot label="row 2" height={92} rounded={14} />
          <div style={{
            height: 92, borderRadius: 14, background: c.primarySoft, color: c.primary,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer', gap: 4,
          }}>
            <Icon name="camera" size={20} />
            <span style={{ fontSize: 10, fontWeight: 700 }}>Add</span>
          </div>
        </div>

        <SectionHeader title="Findings" />
        <Input label="Inspector notes" multiline rows={4} placeholder="Document observations…"
               value="Crop is generally healthy. Mosaic spots seen on 4 plants in row 11 — recommend rogue removal. Curing barn requires ventilation upgrade before harvest peak." />

        <SectionHeader title="Sign-off" />
        <Card padding={14}>
          <div style={{ display: 'flex', gap: 12 }}>
            <Avatar name="Joseph Banda" size={36} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>Inspector Banda</div>
              <div style={{ fontSize: 11, color: c.textMuted }}>09:42 · GPS verified</div>
            </div>
            <Pill tone="success" size="sm" icon={<Icon name="check" size={11} />}>Signed</Pill>
          </div>
        </Card>
      </div>
    </Screen>
  );
}

function ScreenInspectionResult({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false} onClick={() => navigate('inspection')}>Back to list</Button>
        <Button onClick={() => navigate('inspection-detail')}>View report</Button>
      </div>
    }>
      <div style={{ padding: '20px 20px 12px' }}>
        <Card padding={20} style={{
          background: `linear-gradient(180deg, ${c.successSoft} 0%, ${c.surface} 100%)`,
          border: `1px solid ${c.success}30`, textAlign: 'center', overflow: 'hidden',
        }}>
          <div style={{
            width: 84, height: 84, borderRadius: '50%', background: c.success, color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px',
          }}><Icon name="check" size={48} /></div>
          <div style={{ fontSize: 22, fontWeight: 800, color: c.text }}>Inspection passed</div>
          <div style={{ fontSize: 13, color: c.textMuted, marginTop: 6 }}>Report submitted for sync</div>
          <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 12 }}>
            <Pill tone="success" size="sm">5 of 6 checks</Pill>
            <Pill tone="warning" size="sm">1 finding</Pill>
          </div>
        </Card>
      </div>
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14}>
          <FieldRow label="Reference" value="INS-2025-0412" mono />
          <FieldRow label="Grower" value="Mary Phiri · TBZ-04412" />
          <FieldRow label="Type" value="Field inspection" />
          <FieldRow label="Date · GPS" value="12 May · -13.85, 32.50" />
          <FieldRow label="Outcome" value={<Pill tone="success" size="sm">Pass</Pill>} />
          <FieldRow label="Sync" value={<SyncChip status="pending" />} />
        </Card>
      </div>
      <SectionHeader title="Findings" style={{ margin: '14px 24px 0' }} />
      <div style={{ padding: '4px 20px' }}>
        <Card padding={14} accent="gold">
          <div style={{ fontSize: 13, fontWeight: 700, color: c.text }}>Tobacco mosaic on 4 plants · Row 11</div>
          <div style={{ fontSize: 12, color: c.textMuted, marginTop: 4 }}>
            Recommend rogue removal within 3 days. Inspector to revisit on 17 May.
          </div>
          <div style={{ display: 'flex', gap: 6, marginTop: 10 }}>
            <Pill tone="warning" size="sm">Action required</Pill>
            <Pill tone="default" size="sm">Severity · low</Pill>
          </div>
        </Card>
      </div>
    </Screen>
  );
}

function ScreenInspectionDetail({ navigate }) {
  const { c } = useGL();
  return (
    <Screen padded={false} footer={
      <div style={{ display: 'flex', gap: 10 }}>
        <Button variant="outline" full={false} icon={<Icon name="share" size={18} />}>Share</Button>
        <Button onClick={() => navigate('inspection-form')} icon={<Icon name="edit" size={18} />}>Continue inspection</Button>
      </div>
    }>
      <ScreenHeader title="Inspection report" subtitle="INS-2025-0412" onBack={() => navigate('inspection')} />
      <div style={{ padding: '8px 20px' }}>
        <Card padding={14} accent="primary">
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Avatar name="Mary Phiri" size={48} gold />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 700, color: c.text }}>Mary Phiri</div>
              <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace' }}>TBZ-2024-04412</div>
            </div>
            <Pill tone="success" size="sm" icon={<Icon name="check" size={11} />}>Pass</Pill>
          </div>
        </Card>

        <SectionHeader title="Summary" style={{ marginTop: 14 }} />
        <div style={{ display: 'flex', gap: 10 }}>
          <KpiTile label="Checks" value="5/6" tone="primary" />
          <KpiTile label="Area" value="3.85" tone="default" />
          <KpiTile label="Plants" value="14.2k" tone="default" />
          <KpiTile label="Score" value="92" tone="success" />
        </div>

        <SectionHeader title="Compliance" style={{ marginTop: 18 }} />
        <Card padding={4}>
          {[
            { t: 'Plant population within range', v: true },
            { t: 'Inter-row spacing correct', v: true },
            { t: 'No tobacco mosaic visible', v: false },
            { t: 'Pest scouting documented', v: true },
            { t: 'Buffer to water source ≥ 10m', v: true },
            { t: 'No unregistered varieties', v: true },
          ].map((k, i, a) => (
            <React.Fragment key={k.t}>
              <Row icon={<Icon name={k.v ? 'check-circle' : 'x'} size={20} />} title={k.t}
                   tone={k.v ? 'primary' : 'danger'} subtitle={!k.v ? '4 plants on row 11' : undefined}
                   right={<Pill size="sm" tone={k.v ? 'success' : 'danger'}>{k.v ? 'Pass' : 'Finding'}</Pill>} />
              {i < a.length - 1 && <div style={{ height: 1, background: c.outlineSoft }} />}
            </React.Fragment>
          ))}
        </Card>

        <SectionHeader title="Field photos" style={{ marginTop: 18 }} />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
          <ImageSlot label="overview" height={88} rounded={14} />
          <ImageSlot label="row 11" height={88} rounded={14} />
          <ImageSlot label="barn" height={88} rounded={14} />
        </div>

        <SectionHeader title="Inspector notes" style={{ marginTop: 18 }} />
        <Card padding={14}>
          <div style={{ fontSize: 13, color: c.text, lineHeight: 1.55 }}>
            Crop is generally healthy. Mosaic spots seen on 4 plants in row 11 — recommend rogue removal.
            Curing barn requires ventilation upgrade before harvest peak. Will revisit 17 May.
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 14 }}>
            <Avatar name="Joseph Banda" size={28} />
            <div style={{ fontSize: 11, color: c.textMuted }}>Inspector Banda · 12 May 09:42</div>
          </div>
        </Card>

        <SectionHeader title="Audit trail" style={{ marginTop: 18 }} />
        <Card padding={4}>
          <Row icon={<Icon name="check-circle" size={18} />} title="Submitted" subtitle="12 May 09:42 · pending sync" tone="primary" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="edit" size={18} />} title="Form completed" subtitle="12 May 09:38 · Joseph Banda" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="gps" size={18} />} title="GPS verified" subtitle="12 May 09:21 · -13.850, 32.502" />
          <div style={{ height: 1, background: c.outlineSoft }} />
          <Row icon={<Icon name="calendar" size={18} />} title="Scheduled" subtitle="11 May · district office" />
        </Card>
      </div>
    </Screen>
  );
}

function ScreenHighRisk({ navigate }) {
  const { c } = useGL();
  const items = [
    { name: 'Mary Phiri', id: 'TBZ-2024-04412', score: 78, factors: ['Yield/area mismatch', 'Open dispute'], loc: 'Chadiza' },
    { name: 'Patrick Mvula', id: 'TBZ-2024-04055', score: 71, factors: ['Curing capacity unverified', '2 missed inspections'], loc: 'Lundazi' },
    { name: 'Beatrice Mtonga', id: 'TBZ-2023-08910', score: 64, factors: ['No GPS verified', 'Reg. update overdue'], loc: 'Petauke' },
    { name: 'Sam Tembo', id: 'TBZ-2024-04220', score: 58, factors: ['Permit-without-sale flag'], loc: 'Katete' },
  ];
  return (
    <Screen padded={false}>
      <ScreenHeader title="High-risk growers" subtitle="4 flagged" onBack={() => navigate('home')} />
      <div style={{ padding: '8px 20px' }}>
        <Banner tone="danger" icon={<Icon name="warning" size={18} />} title="Field check recommended"
                sub="Scores above 50 trigger an automatic field inspection request." />
      </div>
      <div style={{ padding: '4px 20px' }}>
        <FilterPills options={['All', 'Critical (>70)', 'High (50-70)', 'Watchlist']} value="All" onChange={() => {}} />
      </div>
      <div style={{ padding: '8px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map((g, i) => (
          <Card key={i} padding={14} accent="gold" onClick={() => navigate('grower-details')}>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <div style={{
                width: 56, height: 56, borderRadius: 16,
                background: g.score > 70 ? c.dangerSoft : c.goldSoft,
                color: g.score > 70 ? c.danger : c.goldDeep,
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              }}>
                <div style={{ fontSize: 18, fontWeight: 800, lineHeight: 1 }}>{g.score}</div>
                <div style={{ fontSize: 8, fontWeight: 700, marginTop: 2, letterSpacing: 0.4 }}>RISK</div>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{g.name}</div>
                <div style={{ fontSize: 11, color: c.textMuted, fontFamily: 'ui-monospace, monospace', marginTop: 2 }}>{g.id} · {g.loc}</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 8 }}>
                  {g.factors.map(f => <Pill key={f} size="sm" tone="warning">{f}</Pill>)}
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </Screen>
  );
}

Object.assign(window, {
  ScreenInspectionList, ScreenInspectionSchedule, ScreenInspectionNew,
  ScreenInspectionForm, ScreenInspectionResult, ScreenInspectionDetail,
  ScreenHighRisk,
});
