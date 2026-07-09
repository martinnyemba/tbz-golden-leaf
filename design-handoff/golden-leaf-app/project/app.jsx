// app.jsx — Top-level routing and app composition
const { useState, useEffect, useMemo, useCallback } = React;

// Simple stack-based router
function useRouter(initial = 'home') {
  const [stack, setStack] = useState([initial]);
  const navigate = useCallback((to) => {
    setStack(s => [...s, to]);
    requestAnimationFrame(() => {
      const el = document.querySelector(`[data-screen="${to}"] [data-screen-scroll]`);
      if (el) el.scrollTop = 0;
    });
  }, []);
  const back = useCallback(() => setStack(s => s.length > 1 ? s.slice(0, -1) : s), []);
  return { current: stack[stack.length - 1], navigate, back, stack };
}

const SCREEN_REGISTRY = {
  // Auth
  'onboarding': () => <ScreenOnboarding />,
  'login': () => <ScreenLogin />,
  'login-2fa': () => <ScreenLogin2FA />,
  // Core
  'home': () => <ScreenHome />,
  'menu': () => <ScreenMenu />,
  'search': () => <ScreenSearch2 />,
  // Registration
  'registration': () => <ScreenRegistration />,
  'registration-new-1': () => <ScreenRegistrationNew />,
  'registration-new-2': () => <ScreenRegistrationNew2 />,
  'registration-new-3': () => <ScreenRegistrationNew3 />,
  'registration-new-4': () => <ScreenRegistrationNew4 />,
  'registration-success': () => <ScreenRegistrationSuccess />,
  'grower-details': () => <ScreenGrowerDetails />,
  'scan-id': () => <ScreenScanId />,
  // Inspection
  'inspection': () => <ScreenInspectionList />,
  'inspection-schedule': () => <ScreenInspectionSchedule />,
  'inspection-new': () => <ScreenInspectionNew />,
  'inspection-form': () => <ScreenInspectionForm />,
  'inspection-result': () => <ScreenInspectionResult />,
  'inspection-detail': () => <ScreenInspectionDetail />,
  'high-risk': () => <ScreenHighRisk />,
  // Permits
  'permits': () => <ScreenPermits />,
  'permit-detail': () => <ScreenPermitDetail />,
  'permit-new': () => <ScreenPermitNew />,
  'permit-group': () => <ScreenPermitGroup />,
  'permit-validate': () => <ScreenPermitValidate />,
  // Sales
  'marketing': () => <ScreenMarketing />,
  'sale-detail': () => <ScreenSaleDetail />,
  'sale-capture': () => <ScreenSaleCapture />,
  // Arbitration
  'arbitration': () => <ScreenArbitration />,
  'arbitration-detail': () => <ScreenArbitrationDetail />,
  'arbitration-new': () => <ScreenArbitrationNew />,
  // New June 2026 screens
  'forgot-password': () => <ScreenForgotPassword />,
  'grower-updates': () => <ScreenGrowerUpdatesQueue />,
  'grower-correction': () => <ScreenGrowerCorrection />,
  'corrections': () => <ScreenCorrectionsInbox />,
  'permit-correction': () => <ScreenPermitCorrection />,
  'group-permit-correction': () => <ScreenGroupPermitCorrection />,
  // Profile
  'profile': () => <ScreenProfile2 />,
  'settings': () => <ScreenSettings />,
  'security': () => <ScreenSecurity />,
  'notifications': () => <ScreenNotifications2 />,
  'sync': () => <ScreenSync />,
  'audit': () => <ScreenAudit />,
};

// Aliases for both versions of profile/notif/search (registration screens
// reference 'profile' and 'notifications', auth screens defined the same names).
const ScreenProfile2 = window.ScreenProfile;
const ScreenNotifications2 = window.ScreenNotifications;
const ScreenSearch2 = window.ScreenSearch;

// Provide navigate() globally to all screens (since each screen receives it
// as a prop in its definition, we expose it via context to keep things tidy).
const NavCtx = React.createContext({ navigate: () => {}, back: () => {} });

// Wrap every screen renderer so it receives `navigate` from context.
function ScreenHost({ id }) {
  const nav = React.useContext(NavCtx);
  const Comp = SCREEN_REGISTRY[id];
  if (!Comp) {
    return <div style={{ padding: 24, fontSize: 14 }}>Screen not found: {id}</div>;
  }
  // Each screen factory ignores its props since we pass via context-aware
  // wrappers below. Easiest: monkey-patch with a render-prop
  return React.createElement(SCREEN_WRAPPED[id] || (() => null));
}

// Build wrapped versions that receive navigate as a prop and pass through
const SCREEN_WRAPPED = {};
Object.keys(SCREEN_REGISTRY).forEach((k) => {
  SCREEN_WRAPPED[k] = function Wrapped() {
    const { navigate, back } = React.useContext(NavCtx);
    // Each screen accepts {navigate}; we pass it.
    const ScreenComp = (() => {
      switch (k) {
        case 'onboarding': return ScreenOnboarding;
        case 'login': return ScreenLogin;
        case 'login-2fa': return ScreenLogin2FA;
        case 'home': return ScreenHome;
        case 'menu': return ScreenMenu;
        case 'search': return ScreenSearch;
        case 'registration': return ScreenRegistration;
        case 'registration-new-1': return ScreenRegistrationNew;
        case 'registration-new-2': return ScreenRegistrationNew2;
        case 'registration-new-3': return ScreenRegistrationNew3;
        case 'registration-new-4': return ScreenRegistrationNew4;
        case 'registration-success': return ScreenRegistrationSuccess;
        case 'grower-details': return ScreenGrowerDetails;
        case 'scan-id': return ScreenScanId;
        case 'inspection': return ScreenInspectionList;
        case 'inspection-schedule': return ScreenInspectionSchedule;
        case 'inspection-new': return ScreenInspectionNew;
        case 'inspection-form': return ScreenInspectionForm;
        case 'inspection-result': return ScreenInspectionResult;
        case 'inspection-detail': return ScreenInspectionDetail;
        case 'high-risk': return ScreenHighRisk;
        case 'permits': return ScreenPermits;
        case 'permit-detail': return ScreenPermitDetail;
        case 'permit-new': return ScreenPermitNew;
        case 'permit-group': return ScreenPermitGroup;
        case 'permit-validate': return ScreenPermitValidate;
        case 'marketing': return ScreenMarketing;
        case 'sale-detail': return ScreenSaleDetail;
        case 'sale-capture': return ScreenSaleCapture;
        case 'arbitration': return ScreenArbitration;
        case 'arbitration-detail': return ScreenArbitrationDetail;
        case 'arbitration-new': return ScreenArbitrationNew;
        case 'forgot-password': return ScreenForgotPassword;
        case 'grower-updates': return ScreenGrowerUpdatesQueue;
        case 'grower-correction': return ScreenGrowerCorrection;
        case 'corrections': return ScreenCorrectionsInbox;
        case 'permit-correction': return ScreenPermitCorrection;
        case 'group-permit-correction': return ScreenGroupPermitCorrection;
        case 'profile': return ScreenProfile;
        case 'settings': return ScreenSettings;
        case 'security': return ScreenSecurity;
        case 'notifications': return ScreenNotifications;
        case 'sync': return ScreenSync;
        case 'audit': return ScreenAudit;
        default: return null;
      }
    })();
    if (!ScreenComp) return null;
    return <ScreenComp navigate={navigate} back={back} />;
  };
});

// Map any screen → which tab it belongs to (so the tab bar shows the right active state)
const TAB_FOR_SCREEN = {
  home: 'home', search: 'home',
  permits: 'permits', 'permit-detail': 'permits', 'permit-request': 'permits',
    'permit-validate': 'permits', 'permit-groups': 'permits',
  inspections: 'inspection', 'inspection-detail': 'inspection',
    'inspection-field': 'inspection', 'inspection-nursery': 'inspection',
    'inspection-curing': 'inspection', 'inspection-high-risk': 'inspection',
    'inspection-photo': 'inspection',
  sales: 'scan', 'sales-pending': 'scan', 'sales-capture': 'scan',
  profile: 'profile', settings: 'profile', security: 'profile',
    notifications: 'profile', sync: 'profile', audit: 'profile',
};
// Screens that DON'T show a tab bar (auth, onboarding, full-screen overlays)
const HIDE_TABBAR_ON = new Set([
  'login', 'login-pin', 'login-2fa', 'onboarding', 'forgot', 'forgot-password',
  'registration', 'registration-new', 'registration-new-2', 'registration-new-3',
  'registration-photo', 'scan-id',
]);

// Phone — wraps an Android frame around a single screen with router.
function Phone({ initial = 'home', label, dark = false, hideFrame = false, scale = 1 }) {
  const router = useRouter(initial);
  const navCtx = useMemo(() => ({ navigate: router.navigate, back: router.back }), [router.navigate, router.back]);
  const showTabs = !HIDE_TABBAR_ON.has(router.current);
  const activeTab = TAB_FOR_SCREEN[router.current] || 'home';
  const tabTargets = { home: 'home', permits: 'permits', inspection: 'inspections', profile: 'profile' };

  const inner = (
    <div data-screen={router.current} style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <ScreenHost id={router.current} />
      </div>
      {showTabs && (
        <BottomNav active={activeTab}
                   onChange={(t) => router.navigate(tabTargets[t] || t)}
                   navigate={router.navigate} />
      )}
    </div>
  );

  return (
    <GLContext.Provider value={{ dark, c: GL_TOKENS[dark ? 'dark' : 'light'] }}>
      <NavCtx.Provider value={navCtx}>
        {hideFrame ? (
          <div style={{ width: 412, height: 892, background: dark ? '#0e1814' : '#f7f9f7',
                        borderRadius: 18, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            {inner}
          </div>
        ) : (
          <AndroidDevice dark={dark}>{inner}</AndroidDevice>
        )}
      </NavCtx.Provider>
    </GLContext.Provider>
  );
}

// Tweaks-aware wrapper for the canvas root. Tweaks apply to ALL phones.
function App() {
  const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
    "darkMode": false,
    "primaryFamily": "green",
    "showLabels": true,
    "density": "comfortable"
  }/*EDITMODE-END*/;
  const [tweaks, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const dark = tweaks.darkMode;

  // Inject tweaks into GLContext globally
  React.useEffect(() => {
    document.body.style.background = dark ? '#0a120e' : '#efece4';
  }, [dark]);

  // Group definitions for the canvas
  const flows = [
    {
      id: 'auth',
      title: 'Onboarding & Authentication',
      subtitle: 'First-run carousel → secure login → 2FA',
      screens: [
        { id: 'onboarding', label: '01 · Onboarding' },
        { id: 'login', label: '02 · Sign in' },
        { id: 'forgot-password', label: '03 · Forgot password' },
        { id: 'login-2fa', label: '04 · 2FA verify' },
      ],
    },
    {
      id: 'home',
      title: 'Home & Navigation',
      subtitle: 'Dashboard, menu, universal search',
      screens: [
        { id: 'home', label: '05 · Home dashboard' },
        { id: 'menu', label: '06 · Module menu' },
        { id: 'search', label: '07 · Search' },
        { id: 'corrections', label: '08 · Corrections inbox' },
      ],
    },
    {
      id: 'reg',
      title: 'Grower Registration',
      subtitle: '4-step wizard with NRC scan & GPS',
      screens: [
        { id: 'registration', label: '09 · Grower list' },
        { id: 'scan-id', label: '10 · Scan NRC' },
        { id: 'registration-new-1', label: '11 · Step 1 · Identity' },
        { id: 'registration-new-2', label: '12 · Step 2 · Farm & GPS' },
        { id: 'registration-new-3', label: '13 · Step 3 · Cropping' },
        { id: 'registration-new-4', label: '14 · Step 4 · Review' },
        { id: 'registration-success', label: '15 · Issued' },
        { id: 'grower-details', label: '16 · Grower profile' },
        { id: 'grower-updates', label: '17 · Grower updates queue' },
        { id: 'grower-correction', label: '18 · Fix & resubmit' },
      ],
    },
    {
      id: 'insp',
      title: 'Field Inspections',
      subtitle: 'Schedule → checklist → report',
      screens: [
        { id: 'inspection', label: '19 · Inspection list' },
        { id: 'inspection-schedule', label: '20 · Schedule' },
        { id: 'inspection-new', label: '21 · New inspection' },
        { id: 'inspection-form', label: '22 · Field checklist' },
        { id: 'inspection-result', label: '23 · Outcome' },
        { id: 'inspection-detail', label: '24 · Report detail' },
        { id: 'high-risk', label: '25 · High-risk growers' },
      ],
    },
    {
      id: 'perm',
      title: 'Permits & QR Validation',
      subtitle: 'Issue, group, scan-validate, fix & resubmit',
      screens: [
        { id: 'permits', label: '26 · Permit list' },
        { id: 'permit-detail', label: '27 · Permit ticket' },
        { id: 'permit-new', label: '28 · New permit' },
        { id: 'permit-group', label: '29 · Group permit' },
        { id: 'permit-validate', label: '30 · QR scan' },
        { id: 'permit-correction', label: '31 · Permit correction' },
        { id: 'group-permit-correction', label: '32 · Group permit correction' },
      ],
    },
    {
      id: 'sales',
      title: 'Sales & Marketing',
      subtitle: 'Capture sales, settle, arbitration',
      screens: [
        { id: 'marketing', label: '33 · Sales overview' },
        { id: 'sale-detail', label: '34 · Sale capture' },
        { id: 'sale-capture', label: '35 · New sale' },
        { id: 'arbitration', label: '36 · Arbitration list' },
        { id: 'arbitration-detail', label: '37 · Dispute case' },
        { id: 'arbitration-new', label: '38 · Submit arbitration' },
      ],
    },
    {
      id: 'me',
      title: 'Officer Profile',
      subtitle: 'Account, security, sync, audit',
      screens: [
        { id: 'profile', label: '33 · My profile' },
        { id: 'settings', label: '34 · Personal details' },
        { id: 'security', label: '35 · Security & 2FA' },
        { id: 'notifications', label: '36 · Notifications' },
        { id: 'sync', label: '37 · Offline & sync' },
        { id: 'audit', label: '38 · Activity log' },
      ],
    },
  ];

  return (
    <>
      <DesignCanvas>
        {flows.map((f, i) => (
          <DCSection key={f.id} id={f.id} title={f.title} subtitle={f.subtitle}>
            {f.screens.map(s => (
              <DCArtboard key={s.id} id={s.id} label={s.label} width={412} height={892}>
                <Phone initial={s.id} dark={dark} hideFrame />
              </DCArtboard>
            ))}
          </DCSection>
        ))}
      </DesignCanvas>

      <TweaksPanel title="Tweaks">
        <TweakSection label="Appearance">
          <TweakToggle label="Dark mode" value={tweaks.darkMode}
                       onChange={(v) => setTweak('darkMode', v)} />
        </TweakSection>
        <TweakSection label="Layout">
          <TweakRadio label="Density" value={tweaks.density}
                      options={[{ value: 'comfortable', label: 'Comfy' }, { value: 'compact', label: 'Compact' }]}
                      onChange={(v) => setTweak('density', v)} />
          <TweakToggle label="Artboard labels" value={tweaks.showLabels}
                       onChange={(v) => setTweak('showLabels', v)} />
        </TweakSection>
      </TweaksPanel>
    </>
  );
}

// Mount
ReactDOM.createRoot(document.getElementById('root')).render(<App />);
