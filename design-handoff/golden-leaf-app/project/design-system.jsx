// design-system.jsx
// Golden Leaf — TBZ Field App design system.
// Inspired by the Chefio kit: pill inputs/buttons, soft form fills, generous radii,
// geometric-sans typography. Re-toned for the TBZ green + gold brand and tuned for
// outdoor field use (high contrast, big tap targets).

// ─────────────────────────────────────────────────────────────
// TOKENS
// ─────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────
// TBZ Brand palette (OKLCH source of truth, June 2026)
// WarmIvory #F6F4E6 · WarmBeige #F0E7D3 · GoldenBrown #7E5709
// LeafGreen #26471A · DeepLeafGreen #06320C
// HarvestGold #FFC039 · WarmCharcoal #151107
// ─────────────────────────────────────────────────────────────
const GL_TOKENS = {
  light: {
    // Surfaces — warm ivory / beige base
    bg:           '#F6F4E6',   // Warm Ivory — screen background
    surface:      '#F6F4E6',   // Warm Ivory — cards, sheets
    surfaceAlt:   '#F0E7D3',   // Warm Beige — form fills, input bg, grouped rows
    surfaceMuted: '#E8DCCA',   // Warm Beige darkened — dividers, disabled
    // Text
    text:         '#151107',   // Warm Charcoal — body text
    textMuted:    '#7E5709',   // Golden Brown — captions, hints, metadata
    textSubtle:   '#B08040',   // Golden Brown lightened — placeholder-adjacent
    placeholder:  '#C4A06A',   // Golden Brown at 40% — input placeholder
    // Brand primary — Leaf Green
    primary:      '#26471A',   // Leaf Green — buttons, active nav, links
    primarySoft:  '#DAE8D3',   // Leaf Green @ 20% on WarmIvory — chip bg, icon bg
    primaryDeep:  '#06320C',   // Deep Leaf Green — hero/dark header, splash
    // Gold accent — Harvest Gold
    gold:         '#FFC039',   // Harvest Gold — FAB, badges, CTAs on dark
    goldSoft:     '#FFF0C8',   // Harvest Gold @ 25% — chip bg, highlight panels
    goldDeep:     '#7E5709',   // Golden Brown — text on gold chips
    // Semantic
    success:      '#2E6B20',   // deep green confirmation
    successSoft:  '#DFF2D8',
    warning:      '#FFC039',   // Harvest Gold for warnings
    warningSoft:  '#FFF0C8',
    danger:       '#B33A3A',
    dangerSoft:   '#F4D9D9',
    info:         '#2A5FA8',
    infoSoft:     '#D9E8F8',
    // Strokes — GoldenBrown-derived
    outline:      '#D4C0A0',   // GoldenBrown @ 50% on WarmIvory
    outlineSoft:  '#EDE4D0',   // WarmBeige — subtle dividers
    // Risk
    riskHigh:     '#B33A3A',
    riskMed:      '#FFC039',
    riskLow:      '#2E6B20',
    // Status chips — brand-mapped per spec
    statusActive:    { bg: '#DFF2D8', fg: '#26471A' },
    statusPending:   { bg: '#F0E7D3', fg: '#7E5709' },   // Warm Beige / Golden Brown
    statusFailed:    { bg: '#F4D9D9', fg: '#8E2A2A' },
    statusDraft:     { bg: '#EEEBE0', fg: '#7E5709' },
    statusSynced:    { bg: '#DFF2D8', fg: '#26471A' },
    statusReview:    { bg: '#FFF0C8', fg: '#7E5709' },   // Harvest Gold tint
    statusReturned:  { bg: '#FFF0C8', fg: '#7E5709' },   // RETURNED_FOR_CORRECTION
    statusBlocked:   { bg: '#F0E7D3', fg: '#B33A3A' },
  },
  dark: {
    // Dark: WarmCharcoal bg, DeepLeafGreen surface, HarvestGold primary
    bg:           '#151107',   // Warm Charcoal — screen background
    surface:      '#06320C',   // Deep Leaf Green — cards, sheets
    surfaceAlt:   '#26471A',   // Leaf Green — elevated groups, input fills
    surfaceMuted: '#1A3D13',   // between Deep and Leaf
    text:         '#F6F4E6',   // Warm Ivory — body text
    textMuted:    '#F0E7D3',   // Warm Beige — captions, hints
    textSubtle:   '#C4B898',   // WarmBeige dimmed
    placeholder:  '#8A7A5A',   // muted warm
    // Primary is Harvest Gold on dark
    primary:      '#FFC039',   // Harvest Gold — primary actions on dark
    primarySoft:  '#26471A',   // Leaf Green — selected states, icon bg
    primaryDeep:  '#FFC039',   // same — used for hero elements
    gold:         '#FFC039',   // Harvest Gold
    goldSoft:     '#3A2E10',   // dark gold tint
    goldDeep:     '#FFC039',
    success:      '#7DD49C',
    successSoft:  '#1A3A28',
    warning:      '#FFC039',
    warningSoft:  '#3A2E10',
    danger:       '#E07070',
    dangerSoft:   '#3A1A1A',
    info:         '#7AB0E0',
    infoSoft:     '#1A2A3A',
    outline:      '#3A4A2A',   // Leaf Green dark outline
    outlineSoft:  '#1F2E1A',
    riskHigh:     '#E07070',
    riskMed:      '#FFC039',
    riskLow:      '#7DD49C',
    statusActive:    { bg: '#1A3A28', fg: '#7DD49C' },
    statusPending:   { bg: '#26471A', fg: '#F0E7D3' },
    statusFailed:    { bg: '#3A1A1A', fg: '#E07070' },
    statusDraft:     { bg: '#1F2A1A', fg: '#C4B898' },
    statusSynced:    { bg: '#1A3A28', fg: '#7DD49C' },
    statusReview:    { bg: '#3A2E10', fg: '#FFC039' },
    statusReturned:  { bg: '#3A2E10', fg: '#FFC039' },
    statusBlocked:   { bg: '#3A1A1A', fg: '#E07070' },
  },
};

const useGL = () => {
  const ctx = React.useContext(GLContext);
  return ctx;
};

const GLContext = React.createContext({ c: GL_TOKENS.light, dark: false, density: 'regular' });

// ─────────────────────────────────────────────────────────────
// PRIMITIVES
// ─────────────────────────────────────────────────────────────

// Status / chip pill
function Pill({ tone = 'default', size = 'md', children, icon, style }) {
  const { c } = useGL();
  const tones = {
    default: { bg: c.surfaceAlt, fg: c.textMuted },
    primary: { bg: c.primarySoft, fg: c.primary },
    gold:    { bg: c.goldSoft, fg: c.goldDeep },
    success: { bg: c.successSoft, fg: c.success },
    warning: { bg: c.warningSoft, fg: c.goldDeep },
    danger:  { bg: c.dangerSoft, fg: c.danger },
    info:    { bg: c.infoSoft, fg: c.info },
    active:  { bg: c.statusActive.bg, fg: c.statusActive.fg },
    pending: { bg: c.statusPending.bg, fg: c.statusPending.fg },
    failed:  { bg: c.statusFailed.bg, fg: c.statusFailed.fg },
    draft:   { bg: c.statusDraft.bg, fg: c.statusDraft.fg },
    synced:  { bg: c.statusSynced.bg, fg: c.statusSynced.fg },
    review:  { bg: c.statusReview.bg, fg: c.statusReview.fg },
    returned: { bg: c.statusReturned ? c.statusReturned.bg : c.goldSoft, fg: c.statusReturned ? c.statusReturned.fg : c.goldDeep },
    blocked:  { bg: c.statusBlocked ? c.statusBlocked.bg : c.dangerSoft, fg: c.statusBlocked ? c.statusBlocked.fg : c.danger },
    outline: { bg: 'transparent', fg: c.text, border: `1px solid ${c.outline}` },
  };
  const t = tones[tone] || tones.default;
  const sizes = {
    sm: { fs: 11, py: 3, px: 8, gap: 4, h: 22 },
    md: { fs: 12, py: 4, px: 10, gap: 6, h: 26 },
    lg: { fs: 13, py: 6, px: 12, gap: 6, h: 30 },
  }[size];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: sizes.gap,
      background: t.bg, color: t.fg, border: t.border,
      padding: `${sizes.py}px ${sizes.px}px`, borderRadius: 999,
      fontSize: sizes.fs, fontWeight: 600, letterSpacing: 0.1,
      lineHeight: 1, height: sizes.h, boxSizing: 'border-box',
      ...style,
    }}>
      {icon}
      {children}
    </span>
  );
}

// Button (Chefio-style pill, but using TBZ green)
function Button({
  children, variant = 'primary', size = 'lg', icon, iconRight,
  full = true, disabled, onClick, style,
}) {
  const { c } = useGL();
  const sizes = {
    sm: { h: 36, fs: 13, px: 16, r: 999 },
    md: { h: 44, fs: 14, px: 20, r: 999 },
    lg: { h: 56, fs: 15, px: 24, r: 999 },
    xl: { h: 64, fs: 16, px: 28, r: 999 },
  }[size];
  const variants = {
    primary:   { bg: c.primary, fg: '#fff', border: 'none' },
    gold:      { bg: c.gold, fg: '#1A1308', border: 'none' },
    secondary: { bg: c.primarySoft, fg: c.primary, border: 'none' },
    outline:   { bg: 'transparent', fg: c.primary, border: `1.5px solid ${c.primary}` },
    ghost:     { bg: 'transparent', fg: c.text, border: 'none' },
    danger:    { bg: c.danger, fg: '#fff', border: 'none' },
    dangerOutline: { bg: 'transparent', fg: c.danger, border: `1.5px solid ${c.danger}` },
    surface:   { bg: c.surface, fg: c.text, border: `1px solid ${c.outline}` },
  };
  const v = variants[variant];
  return (
    <button
      onClick={disabled ? undefined : onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        gap: 8, height: sizes.h, padding: `0 ${sizes.px}px`,
        background: v.bg, color: v.fg, border: v.border, borderRadius: sizes.r,
        fontSize: sizes.fs, fontWeight: 700, letterSpacing: 0.2,
        width: full ? '100%' : 'auto',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.5 : 1,
        fontFamily: 'inherit',
        boxShadow: variant === 'primary' ? '0 6px 16px -8px rgba(11,107,58,0.6)' : 'none',
        transition: 'transform 0.1s, opacity 0.15s',
        ...style,
      }}
      onMouseDown={(e) => !disabled && (e.currentTarget.style.transform = 'scale(0.98)')}
      onMouseUp={(e) => (e.currentTarget.style.transform = '')}
      onMouseLeave={(e) => (e.currentTarget.style.transform = '')}
    >
      {icon}
      {children}
      {iconRight}
    </button>
  );
}

// Input — pill-shaped Chefio-style, with label, icon, helper
function Input({
  label, value, onChange, placeholder, icon, suffix, type = 'text',
  helper, error, success, focused: focusProp, multiline, rows = 3,
  readOnly, required, style,
}) {
  const { c } = useGL();
  const [focused, setFocused] = React.useState(false);
  const isFilled = value != null && String(value).length > 0;
  const showFocus = focused || focusProp;
  const borderColor = error ? c.danger : success ? c.success : showFocus ? c.primary : c.outline;
  const bg = c.surfaceAlt;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, ...style }}>
      {label && (
        <label style={{ fontSize: 13, fontWeight: 600, color: c.textMuted, paddingLeft: 4 }}>
          {label} {required && <span style={{ color: c.danger }}>*</span>}
        </label>
      )}
      <div style={{
        display: 'flex', alignItems: multiline ? 'flex-start' : 'center', gap: 12,
        background: bg, borderRadius: multiline ? 20 : 32,
        padding: multiline ? '14px 18px' : '0 18px',
        height: multiline ? 'auto' : 56, minHeight: multiline ? 96 : 56,
        border: `1.5px solid ${showFocus || error || success ? borderColor : 'transparent'}`,
        transition: 'border-color 0.15s',
      }}>
        {icon && <div style={{ flexShrink: 0, color: c.textMuted, marginTop: multiline ? 2 : 0 }}>{icon}</div>}
        {multiline ? (
          <textarea
            rows={rows}
            {...(onChange ? { value: value || '', onChange } : { defaultValue: value || '' })}
            onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
            placeholder={placeholder} readOnly={readOnly}
            style={{
              flex: 1, border: 'none', outline: 'none', background: 'transparent',
              fontFamily: 'inherit', fontSize: 15, fontWeight: 500, color: c.text,
              resize: 'none', lineHeight: 1.5,
            }}
          />
        ) : (
          <input
            type={type}
            {...(onChange ? { value: value || '', onChange } : { defaultValue: value || '' })}
            onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
            placeholder={placeholder} readOnly={readOnly}
            style={{
              flex: 1, border: 'none', outline: 'none', background: 'transparent',
              fontFamily: 'inherit', fontSize: 15, fontWeight: 500, color: c.text,
              minWidth: 0,
            }}
          />
        )}
        {suffix && <div style={{ flexShrink: 0, color: c.textMuted }}>{suffix}</div>}
      </div>
      {(helper || error) && (
        <div style={{
          fontSize: 12, color: error ? c.danger : c.textMuted, paddingLeft: 16,
        }}>{error || helper}</div>
      )}
    </div>
  );
}

// Select that visually matches Input
function Select({ label, value, onChange, options, placeholder, required, helper }) {
  const { c } = useGL();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {label && (
        <label style={{ fontSize: 13, fontWeight: 600, color: c.textMuted, paddingLeft: 4 }}>
          {label} {required && <span style={{ color: c.danger }}>*</span>}
        </label>
      )}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, background: c.surfaceAlt,
        borderRadius: 32, padding: '0 18px', height: 56, position: 'relative',
      }}>
        <select
          {...(onChange ? { value: value || '', onChange } : { defaultValue: value || '' })}
          style={{
            flex: 1, border: 'none', outline: 'none', background: 'transparent',
            fontFamily: 'inherit', fontSize: 15, fontWeight: 500,
            color: value ? c.text : c.placeholder,
            appearance: 'none', WebkitAppearance: 'none',
            paddingRight: 16,
          }}
        >
          {placeholder && <option value="">{placeholder}</option>}
          {options.map((o) => {
            const v = typeof o === 'string' ? o : o.value;
            const l = typeof o === 'string' ? o : o.label;
            return <option key={v} value={v}>{l}</option>;
          })}
        </select>
        <Icon name="chevron-down" size={18} style={{ color: c.textMuted, pointerEvents: 'none' }} />
      </div>
      {helper && <div style={{ fontSize: 12, color: c.textMuted, paddingLeft: 16 }}>{helper}</div>}
    </div>
  );
}

// Card
function Card({ children, padding = 16, onClick, style, elevated = false, accent }) {
  const { c } = useGL();
  return (
    <div
      onClick={onClick}
      style={{
        background: c.surface, borderRadius: 20, padding,
        border: `1px solid ${c.outlineSoft}`,
        boxShadow: elevated ? '0 8px 24px -16px rgba(15,40,25,0.18)' : 'none',
        cursor: onClick ? 'pointer' : 'default',
        position: 'relative', overflow: 'hidden',
        ...style,
      }}
    >
      {accent && (
        <div style={{
          position: 'absolute', left: 0, top: 0, bottom: 0, width: 4,
          background: accent === 'gold' ? c.gold : c.primary,
        }} />
      )}
      {children}
    </div>
  );
}

// Avatar with initials
function Avatar({ name = 'TB', size = 40, src, gold }) {
  const { c } = useGL();
  const initials = name.split(' ').map(s => s[0]).slice(0, 2).join('').toUpperCase();
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: gold ? c.gold : c.primary, color: gold ? '#1A1308' : '#fff',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      fontWeight: 700, fontSize: size * 0.36, flexShrink: 0,
      backgroundImage: src ? `url(${src})` : 'none',
      backgroundSize: 'cover', backgroundPosition: 'center',
    }}>
      {!src && initials}
    </div>
  );
}

// Header bar (stays inside the screen content; not the Android app bar)
function ScreenHeader({ title, subtitle, onBack, right, large = false, hero = false, sticky = true }) {
  const { c } = useGL();
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: hero ? '18px 20px 8px' : '14px 20px',
      background: hero ? 'transparent' : c.bg,
      position: sticky ? 'sticky' : 'static', top: 0, zIndex: 10,
      borderBottom: hero ? 'none' : `1px solid ${c.outlineSoft}`,
    }}>
      {onBack && (
        <button onClick={onBack} style={{
          width: 40, height: 40, borderRadius: '50%', border: 'none',
          background: c.surfaceAlt, display: 'flex', alignItems: 'center',
          justifyContent: 'center', cursor: 'pointer', flexShrink: 0,
          color: c.text,
        }}>
          <Icon name="arrow-left" size={20} />
        </button>
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: large ? 22 : 17, fontWeight: 700, color: c.text,
          letterSpacing: 0.1, lineHeight: 1.2,
        }}>{title}</div>
        {subtitle && (
          <div style={{ fontSize: 12, color: c.textMuted, marginTop: 2 }}>{subtitle}</div>
        )}
      </div>
      {right}
    </div>
  );
}

// Section heading
function SectionHeader({ title, action, onAction, style }) {
  const { c } = useGL();
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
      padding: '8px 4px', ...style,
    }}>
      <div style={{ fontSize: 15, fontWeight: 700, color: c.text, letterSpacing: 0.1 }}>{title}</div>
      {action && (
        <button onClick={onAction} style={{
          background: 'none', border: 'none', color: c.primary, fontSize: 13,
          fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit',
        }}>{action}</button>
      )}
    </div>
  );
}

// KPI tile
function KpiTile({ label, value, tone = 'default', icon, onClick, style }) {
  const { c } = useGL();
  const tones = {
    default: { bg: c.surface, fg: c.text, accent: c.primary },
    primary: { bg: c.primarySoft, fg: c.primary, accent: c.primary },
    gold:    { bg: c.goldSoft, fg: c.goldDeep, accent: c.gold },
    danger:  { bg: c.dangerSoft, fg: c.danger, accent: c.danger },
    success: { bg: c.successSoft, fg: c.success, accent: c.success },
    info:    { bg: c.infoSoft, fg: c.info, accent: c.info },
  };
  const t = tones[tone];
  return (
    <div onClick={onClick} style={{
      flex: 1, background: t.bg, borderRadius: 16, padding: '14px 16px',
      cursor: onClick ? 'pointer' : 'default', minWidth: 0,
      border: tone === 'default' ? `1px solid ${c.outlineSoft}` : 'none',
      ...style,
    }}>
      {icon && <div style={{ marginBottom: 6, color: t.accent }}>{icon}</div>}
      <div style={{ fontSize: 24, fontWeight: 800, color: t.fg, letterSpacing: -0.3, lineHeight: 1.1 }}>{value}</div>
      <div style={{ fontSize: 11, fontWeight: 600, color: tone === 'default' ? c.textMuted : t.fg,
                     opacity: tone === 'default' ? 1 : 0.75,
                     marginTop: 4, textTransform: 'uppercase', letterSpacing: 0.5 }}>{label}</div>
    </div>
  );
}

// Tile — large action tile for dashboards
function Tile({ icon, label, sub, onClick, tone = 'primary', badge }) {
  const { c } = useGL();
  const bg = tone === 'gold' ? c.goldSoft : tone === 'primary' ? c.primarySoft : c.surfaceAlt;
  const fg = tone === 'gold' ? c.goldDeep : tone === 'primary' ? c.primary : c.text;
  return (
    <button onClick={onClick} style={{
      flex: 1, minWidth: 0, background: c.surface, border: `1px solid ${c.outlineSoft}`,
      borderRadius: 20, padding: 16, display: 'flex', flexDirection: 'column',
      alignItems: 'flex-start', gap: 10, cursor: 'pointer', fontFamily: 'inherit',
      textAlign: 'left', position: 'relative',
    }}>
      <div style={{
        width: 44, height: 44, borderRadius: 14, background: bg, color: fg,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{icon}</div>
      <div style={{ width: '100%' }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: c.text }}>{label}</div>
        {sub && <div style={{ fontSize: 11, color: c.textMuted, marginTop: 2 }}>{sub}</div>}
      </div>
      {badge != null && (
        <div style={{
          position: 'absolute', top: 12, right: 12, background: c.gold, color: '#1A1308',
          fontSize: 11, fontWeight: 800, padding: '2px 8px', borderRadius: 999, minWidth: 20,
          textAlign: 'center',
        }}>{badge}</div>
      )}
    </button>
  );
}

// Row item — list row with chevron
function Row({ icon, title, subtitle, right, onClick, danger, tone = 'default' }) {
  const { c } = useGL();
  const fg = danger ? c.danger : c.text;
  const iconBg = tone === 'gold' ? c.goldSoft : tone === 'danger' ? c.dangerSoft
                : tone === 'primary' ? c.primarySoft : c.surfaceAlt;
  const iconFg = tone === 'gold' ? c.goldDeep : tone === 'danger' ? c.danger
                : tone === 'primary' ? c.primary : c.textMuted;
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 14, padding: '10px 4px',
      cursor: onClick ? 'pointer' : 'default',
    }}>
      {icon && (
        <div style={{
          width: 40, height: 40, borderRadius: 12, background: iconBg, color: iconFg,
          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        }}>{icon}</div>
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: fg }}>{title}</div>
        {subtitle && (
          <div style={{ fontSize: 12, color: c.textMuted, marginTop: 1, whiteSpace: 'nowrap',
                        overflow: 'hidden', textOverflow: 'ellipsis' }}>{subtitle}</div>
        )}
      </div>
      {right || (onClick && <Icon name="chevron-right" size={18} style={{ color: c.textSubtle }} />)}
    </div>
  );
}

// Filter pills row
function FilterPills({ options, value, onChange, style }) {
  const { c } = useGL();
  return (
    <div style={{ display: 'flex', gap: 8, overflowX: 'auto',
                  paddingBottom: 4, scrollbarWidth: 'none', ...style }}>
      {options.map((o) => {
        const v = typeof o === 'string' ? o : o.value;
        const l = typeof o === 'string' ? o : o.label;
        const active = value === v;
        return (
          <button key={v} onClick={() => onChange(v)} style={{
            background: active ? c.primary : c.surfaceAlt, color: active ? '#fff' : c.text,
            border: 'none', padding: '8px 16px', borderRadius: 999, fontSize: 13, fontWeight: 600,
            whiteSpace: 'nowrap', cursor: 'pointer', flexShrink: 0, fontFamily: 'inherit',
            transition: 'all 0.15s',
          }}>{l}</button>
        );
      })}
    </div>
  );
}

// Search bar
function SearchBar({ value, onChange, placeholder = 'Search…', onScan, style }) {
  const { c } = useGL();
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12, background: c.surfaceAlt,
      borderRadius: 32, padding: '0 18px', height: 52, ...style,
    }}>
      <Icon name="search" size={20} style={{ color: c.textMuted, flexShrink: 0 }} />
      <input
        {...(onChange ? { value: value || '', onChange } : { defaultValue: value || '' })}
        placeholder={placeholder}
        style={{
          flex: 1, border: 'none', outline: 'none', background: 'transparent',
          fontFamily: 'inherit', fontSize: 14, fontWeight: 500, color: c.text, minWidth: 0,
        }}
      />
      {onScan && (
        <button onClick={onScan} style={{
          width: 32, height: 32, borderRadius: '50%', border: 'none',
          background: c.primary, color: '#fff', cursor: 'pointer', flexShrink: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon name="qr" size={16} />
        </button>
      )}
    </div>
  );
}

// Toggle
function Toggle({ value, onChange }) {
  const { c } = useGL();
  return (
    <button onClick={() => onChange(!value)} style={{
      width: 44, height: 26, borderRadius: 999, border: 'none',
      background: value ? c.primary : c.outline, position: 'relative',
      cursor: 'pointer', padding: 0, transition: 'background 0.15s',
    }}>
      <span style={{
        position: 'absolute', top: 3, left: value ? 21 : 3, width: 20, height: 20,
        borderRadius: '50%', background: '#fff', transition: 'left 0.15s',
        boxShadow: '0 1px 3px rgba(0,0,0,0.25)',
      }} />
    </button>
  );
}

// Row with toggle on the right
function ToggleRow({ icon, title, subtitle, defaultOn = false, tone = 'default' }) {
  const [on, setOn] = React.useState(defaultOn);
  return <Row icon={icon} title={title} subtitle={subtitle} tone={tone}
              right={<Toggle value={on} onChange={setOn} />} />;
}

// Bottom nav (Chefio-style: pill tab bar with center scan FAB)
function BottomNav({ active, onChange, navigate }) {
  const { c } = useGL();
  const items = [
    { id: 'home', icon: 'home', label: 'Home' },
    { id: 'permits', icon: 'permit', label: 'Permits' },
    { id: 'scan', icon: 'scan', label: '', center: true },
    { id: 'inspection', icon: 'inspection', label: 'Inspect' },
    { id: 'profile', icon: 'profile', label: 'Profile' },
  ];
  return (
    <div style={{
      background: c.surface, borderTop: `1px solid ${c.outlineSoft}`,
      padding: '8px 12px 10px', display: 'flex', alignItems: 'center',
      justifyContent: 'space-between', gap: 4, position: 'relative',
    }}>
      {items.map((it) => {
        const isActive = active === it.id;
        if (it.center) {
          return (
            <button key={it.id} onClick={() => navigate('sales')} style={{
              width: 56, height: 56, borderRadius: '50%', border: 'none',
              background: c.primary, color: '#fff', cursor: 'pointer',
              boxShadow: '0 8px 20px -8px rgba(11,107,58,0.6)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              marginTop: -22,
            }}>
              <Icon name="scan" size={26} />
            </button>
          );
        }
        return (
          <button key={it.id} onClick={() => onChange(it.id)} style={{
            flex: 1, background: 'none', border: 'none', display: 'flex',
            flexDirection: 'column', alignItems: 'center', gap: 3, padding: '6px 0',
            color: isActive ? c.primary : c.textSubtle, cursor: 'pointer',
            fontFamily: 'inherit',
          }}>
            <Icon name={it.icon} size={22} filled={isActive} />
            <span style={{ fontSize: 10, fontWeight: 600 }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// Two-column field row
function FieldRow({ label, value, mono }) {
  const { c } = useGL();
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, padding: '8px 0' }}>
      <div style={{ fontSize: 13, color: c.textMuted, fontWeight: 500 }}>{label}</div>
      <div style={{ fontSize: 13, color: c.text, fontWeight: 600, textAlign: 'right',
                    fontFamily: mono ? 'ui-monospace, monospace' : 'inherit' }}>{value}</div>
    </div>
  );
}

// Empty state
function EmptyState({ icon, title, sub, action }) {
  const { c } = useGL();
  return (
    <div style={{
      padding: '40px 24px', textAlign: 'center', display: 'flex', flexDirection: 'column',
      alignItems: 'center', gap: 12,
    }}>
      <div style={{
        width: 72, height: 72, borderRadius: '50%', background: c.surfaceAlt,
        color: c.textMuted, display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{icon}</div>
      <div>
        <div style={{ fontSize: 16, fontWeight: 700, color: c.text }}>{title}</div>
        {sub && <div style={{ fontSize: 13, color: c.textMuted, marginTop: 4, maxWidth: 260 }}>{sub}</div>}
      </div>
      {action}
    </div>
  );
}

// Banner / inline notification
function Banner({ tone = 'info', title, sub, icon, style }) {
  const { c } = useGL();
  const tones = {
    info:    { bg: c.infoSoft, fg: c.info },
    warning: { bg: c.warningSoft, fg: c.goldDeep },
    danger:  { bg: c.dangerSoft, fg: c.danger },
    success: { bg: c.successSoft, fg: c.success },
    primary: { bg: c.primarySoft, fg: c.primary },
  };
  const t = tones[tone];
  return (
    <div style={{
      display: 'flex', gap: 12, background: t.bg, padding: 14, borderRadius: 14,
      alignItems: 'flex-start', ...style,
    }}>
      {icon && <div style={{ color: t.fg, flexShrink: 0, marginTop: 1 }}>{icon}</div>}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: t.fg }}>{title}</div>
        {sub && <div style={{ fontSize: 12, color: t.fg, opacity: 0.85, marginTop: 2 }}>{sub}</div>}
      </div>
    </div>
  );
}

// QR placeholder — striped square
function QRPlaceholder({ size = 160 }) {
  const { c } = useGL();
  // Generate a 21x21 pseudo-QR
  const cells = [];
  for (let y = 0; y < 21; y++) {
    for (let x = 0; x < 21; x++) {
      // finder squares
      const inFinder = (xi, yi) => (xi < 7 && yi < 7) || (xi > 13 && yi < 7) || (xi < 7 && yi > 13);
      const inFinderInner = (xi, yi) => (xi >= 2 && xi < 5 && yi >= 2 && yi < 5)
        || (xi >= 16 && xi < 19 && yi >= 2 && yi < 5)
        || (xi >= 2 && xi < 5 && yi >= 16 && yi < 19);
      const inFinderRing = (xi, yi) =>
        ((xi === 0 || xi === 6) && yi >= 0 && yi <= 6) ||
        ((yi === 0 || yi === 6) && xi >= 0 && xi <= 6) ||
        ((xi === 14 || xi === 20) && yi >= 0 && yi <= 6) ||
        ((yi === 0 || yi === 6) && xi >= 14 && xi <= 20) ||
        ((xi === 0 || xi === 6) && yi >= 14 && yi <= 20) ||
        ((yi === 14 || yi === 20) && xi >= 0 && xi <= 6);
      let fill = false;
      if (inFinder(x, y)) {
        fill = inFinderRing(x, y) || inFinderInner(x, y);
      } else {
        fill = ((x * 7 + y * 3 + (x ^ y)) % 3) === 0;
      }
      if (fill) cells.push({ x, y });
    }
  }
  const cs = size / 21;
  return (
    <div style={{
      width: size, height: size, background: '#fff', borderRadius: 12,
      padding: 6, boxSizing: 'border-box',
      border: `1px solid ${c.outline}`,
    }}>
      <svg viewBox="0 0 21 21" width="100%" height="100%">
        {cells.map((cl, i) => (
          <rect key={i} x={cl.x} y={cl.y} width={1} height={1} fill="#16261C" />
        ))}
      </svg>
    </div>
  );
}

// Image placeholder — striped
function ImageSlot({ label = 'photo', height = 120, style, rounded = 16 }) {
  const { c } = useGL();
  return (
    <div style={{
      height, borderRadius: rounded, background: `repeating-linear-gradient(
        135deg, ${c.surfaceAlt}, ${c.surfaceAlt} 8px, ${c.surfaceMuted} 8px, ${c.surfaceMuted} 16px
      )`, display: 'flex', alignItems: 'center', justifyContent: 'center',
      color: c.textMuted, fontFamily: 'ui-monospace, monospace', fontSize: 11,
      letterSpacing: 0.3, border: `1px dashed ${c.outline}`,
      ...style,
    }}>
      {label}
    </div>
  );
}

// Sync chip in header
function SyncChip({ status = 'synced', count = 0 }) {
  const { c } = useGL();
  const cfg = {
    synced:  { bg: c.successSoft, fg: c.success, dot: c.success, label: 'Synced' },
    pending: { bg: c.goldSoft, fg: c.goldDeep, dot: c.gold, label: count ? `${count} pending` : 'Pending' },
    offline: { bg: c.surfaceAlt, fg: c.textMuted, dot: c.textSubtle, label: 'Offline' },
    syncing: { bg: c.infoSoft, fg: c.info, dot: c.info, label: 'Syncing…' },
    failed:  { bg: c.dangerSoft, fg: c.danger, dot: c.danger, label: 'Failed' },
  }[status] || { bg: 'transparent', fg: '#888', dot: '#888', label: status };
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6, background: cfg.bg, color: cfg.fg,
      padding: '5px 10px 5px 8px', borderRadius: 999, fontSize: 11, fontWeight: 700,
    }}>
      <span style={{ width: 7, height: 7, borderRadius: '50%', background: cfg.dot,
                     boxShadow: status === 'syncing' ? `0 0 0 0 ${cfg.dot}` : 'none',
                     animation: status === 'syncing' ? 'glPulse 1.4s infinite' : 'none' }} />
      {cfg.label}
    </span>
  );
}

// Stepper indicator (1 of 3, etc)
function Stepper({ step, total, labels }) {
  const { c } = useGL();
  return (
    <div style={{ padding: '4px 20px 12px' }}>
      <div style={{ display: 'flex', gap: 6 }}>
        {Array.from({ length: total }, (_, i) => (
          <div key={i} style={{
            flex: 1, height: 4, borderRadius: 2,
            background: i < step ? c.primary : c.outline,
          }} />
        ))}
      </div>
      <div style={{ fontSize: 11, fontWeight: 600, color: c.textMuted, marginTop: 8,
                    textTransform: 'uppercase', letterSpacing: 0.5 }}>
        Step {step} of {total}{labels && ` · ${labels[step - 1]}`}
      </div>
    </div>
  );
}

// Fab (when not inside the bottom nav)
function FAB({ icon, label, onClick, style }) {
  const { c } = useGL();
  return (
    <button onClick={onClick} style={{
      position: 'absolute', right: 20, bottom: 88, height: 56,
      padding: label ? '0 22px' : 0, width: label ? 'auto' : 56,
      background: c.primary, color: '#fff', border: 'none', borderRadius: 999,
      display: 'flex', alignItems: 'center', gap: 10,
      boxShadow: '0 12px 28px -10px rgba(11,107,58,0.55)',
      fontFamily: 'inherit', fontWeight: 700, fontSize: 14, cursor: 'pointer',
      zIndex: 5, ...style,
    }}>
      {icon}
      {label}
    </button>
  );
}

// Page-level scrolling container with bg
function Screen({ children, style, padded = true, footer, scroll = true }) {
  const { c } = useGL();
  return (
    <div style={{
      flex: 1, background: c.bg, display: 'flex', flexDirection: 'column',
      overflow: 'hidden', minHeight: 0,
    }}>
      <div style={{
        flex: 1, overflowY: scroll ? 'auto' : 'hidden',
        padding: padded ? '0 0 24px' : 0, ...style,
      }}>
        {children}
      </div>
      {footer && (
        <div style={{ padding: '12px 20px 16px', borderTop: `1px solid ${c.outlineSoft}`, background: c.surface }}>
          {footer}
        </div>
      )}
    </div>
  );
}

// TBZ logo mark — uses the official TBZ logo image
function TbzMark({ size = 48 }) {
  return (
    <img
      src="assets/tbz-logo.png"
      alt="Tobacco Board of Zambia"
      width={size}
      height={size}
      style={{ width: size, height: size, objectFit: 'contain', flexShrink: 0, display: 'block' }}
    />
  );
}

// TBZ word lockup
function TbzLockup({ size = 'md' }) {
  const { c } = useGL();
  const sizes = { sm: { mark: 32, h: 13, t: 9 }, md: { mark: 44, h: 16, t: 11 }, lg: { mark: 64, h: 22, t: 13 } };
  const s = sizes[size];
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <TbzMark size={s.mark} />
      <div>
        <div style={{ fontSize: s.h, fontWeight: 800, color: c.text, letterSpacing: 0.3, lineHeight: 1.1 }}>
          TOBACCO BOARD OF ZAMBIA
        </div>
        <div style={{ fontSize: s.t, fontWeight: 600, color: c.gold, letterSpacing: 0.2, marginTop: 2,
                      fontStyle: 'italic' }}>
          Tobacco, Our Green Gold
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  GL_TOKENS, GLContext, useGL,
  Pill, Button, Input, Select, Card, Avatar, ScreenHeader, SectionHeader,
  KpiTile, Tile, Row, ToggleRow, FilterPills, SearchBar, Toggle, BottomNav, FieldRow,
  EmptyState, Banner, QRPlaceholder, ImageSlot, SyncChip, Stepper, FAB, Screen,
  TbzMark, TbzLockup,
});
