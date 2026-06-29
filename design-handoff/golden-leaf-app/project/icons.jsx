// icons.jsx
// Single source for all icons in the app. Stroke-based, 24×24, currentColor.
// Uses a `name` prop for switching. `filled` flips a few icons to filled state.

function Icon({ name, size = 24, style, filled = false }) {
  const stroke = 'currentColor';
  const sw = 1.8;
  const props = {
    width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke, strokeWidth: sw, strokeLinecap: 'round', strokeLinejoin: 'round',
    style, 'aria-hidden': true,
  };
  switch (name) {
    case 'home': return filled ? (
      <svg {...props} fill={stroke} stroke="none">
        <path d="M3 11l9-7 9 7v9a2 2 0 0 1-2 2h-4v-7h-6v7H5a2 2 0 0 1-2-2v-9z"/>
      </svg>
    ) : (
      <svg {...props}>
        <path d="M3 11l9-7 9 7"/><path d="M5 10v10a1 1 0 0 0 1 1h4v-7h4v7h4a1 1 0 0 0 1-1V10"/>
      </svg>
    );
    case 'permit': return (
      <svg {...props}>
        <rect x="4" y="3" width="16" height="18" rx="2"/>
        <path d="M8 8h8M8 12h8M8 16h5"/>
        <circle cx="17" cy="17" r="0.5" fill={stroke}/>
      </svg>
    );
    case 'inspection': return filled ? (
      <svg {...props}>
        <path d="M11 4a7 7 0 1 1-4.95 11.95L3 19l3.05-3.05A7 7 0 0 1 11 4z" fill={stroke} stroke="none" opacity="0.18"/>
        <circle cx="11" cy="11" r="7"/><path d="M16 16l5 5"/>
      </svg>
    ) : (
      <svg {...props}><circle cx="11" cy="11" r="7"/><path d="M16 16l5 5"/></svg>
    );
    case 'profile': return filled ? (
      <svg {...props} fill={stroke} stroke="none">
        <circle cx="12" cy="8" r="4"/><path d="M4 21v-1a8 8 0 0 1 16 0v1z"/>
      </svg>
    ) : (
      <svg {...props}><circle cx="12" cy="8" r="4"/><path d="M4 21v-1a8 8 0 0 1 16 0v1"/></svg>
    );
    case 'scan': return (
      <svg {...props}>
        <path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3"/>
        <path d="M4 12h16"/>
      </svg>
    );
    case 'qr': return (
      <svg {...props}>
        <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
        <rect x="3" y="14" width="7" height="7" rx="1"/>
        <path d="M14 14h3v3M21 14v3M14 21h7"/>
      </svg>
    );
    case 'leaf': return (
      <svg {...props}>
        <path d="M21 3c0 9-6 16-14 18 0-9 6-16 14-18z"/>
        <path d="M3 21l9-9"/>
      </svg>
    );
    case 'plant': return (
      <svg {...props}>
        <path d="M12 21v-7"/>
        <path d="M12 14c-3 0-7-2-7-7 4 0 7 3 7 7z"/>
        <path d="M12 14c3 0 7-2 7-7-4 0-7 3-7 7z"/>
      </svg>
    );
    case 'barn': return (
      <svg {...props}>
        <path d="M3 21V10l9-6 9 6v11"/>
        <path d="M3 21h18M9 21v-7h6v7"/>
        <path d="M3 14h18"/>
      </svg>
    );
    case 'bale': return (
      <svg {...props}>
        <rect x="4" y="6" width="16" height="13" rx="2"/>
        <path d="M4 10h16M4 14h16M9 6v13M15 6v13"/>
      </svg>
    );
    case 'truck': return (
      <svg {...props}>
        <path d="M3 7h11v9H3z"/><path d="M14 10h4l3 3v3h-7"/>
        <circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/>
      </svg>
    );
    case 'map-pin': return (
      <svg {...props}>
        <path d="M12 22s7-7.5 7-13a7 7 0 1 0-14 0c0 5.5 7 13 7 13z"/>
        <circle cx="12" cy="9" r="2.5"/>
      </svg>
    );
    case 'gps': return (
      <svg {...props}>
        <circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/>
        <circle cx="12" cy="12" r="8"/>
      </svg>
    );
    case 'calendar': return (
      <svg {...props}>
        <rect x="3" y="5" width="18" height="16" rx="2"/>
        <path d="M3 10h18M8 3v4M16 3v4"/>
      </svg>
    );
    case 'clock': return (<svg {...props}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>);
    case 'bell': return (
      <svg {...props}>
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8z"/>
        <path d="M10 21a2 2 0 0 0 4 0"/>
      </svg>
    );
    case 'search': return (<svg {...props}><circle cx="11" cy="11" r="7"/><path d="M16 16l5 5"/></svg>);
    case 'plus': return (<svg {...props}><path d="M12 5v14M5 12h14"/></svg>);
    case 'minus': return (<svg {...props}><path d="M5 12h14"/></svg>);
    case 'x': return (<svg {...props}><path d="M6 6l12 12M18 6L6 18"/></svg>);
    case 'check': return (<svg {...props}><path d="M5 12l5 5L20 7"/></svg>);
    case 'check-circle': return (
      <svg {...props}><circle cx="12" cy="12" r="9"/><path d="M8 12l3 3 5-6"/></svg>
    );
    case 'arrow-left': return (<svg {...props}><path d="M15 18l-6-6 6-6"/></svg>);
    case 'arrow-right': return (<svg {...props}><path d="M9 6l6 6-6 6"/></svg>);
    case 'chevron-right': return (<svg {...props}><path d="M9 6l6 6-6 6"/></svg>);
    case 'chevron-left': return (<svg {...props}><path d="M15 6l-6 6 6 6"/></svg>);
    case 'chevron-down': return (<svg {...props}><path d="M6 9l6 6 6-6"/></svg>);
    case 'chevron-up': return (<svg {...props}><path d="M6 15l6-6 6 6"/></svg>);
    case 'menu': return (<svg {...props}><path d="M4 7h16M4 12h16M4 17h16"/></svg>);
    case 'more': return (
      <svg {...props}><circle cx="5" cy="12" r="1.2" fill={stroke}/><circle cx="12" cy="12" r="1.2" fill={stroke}/><circle cx="19" cy="12" r="1.2" fill={stroke}/></svg>
    );
    case 'lock': return (
      <svg {...props}><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 1 1 8 0v3"/></svg>
    );
    case 'unlock': return (
      <svg {...props}><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 7-2.6"/></svg>
    );
    case 'mail': return (
      <svg {...props}><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 7 9-7"/></svg>
    );
    case 'phone': return (
      <svg {...props}><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A14 14 0 0 1 4 7a2 2 0 0 1 2-2"/></svg>
    );
    case 'eye': return (
      <svg {...props}><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>
    );
    case 'eye-off': return (
      <svg {...props}><path d="M3 3l18 18"/><path d="M10 5.5A10 10 0 0 1 22 12a13 13 0 0 1-3 3.5M6 7.5A14 14 0 0 0 2 12s4 7 10 7c1.5 0 2.9-.3 4.2-.8"/><path d="M9.5 9.5a3 3 0 0 0 4.2 4.2"/></svg>
    );
    case 'shield': return (
      <svg {...props}><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3z"/></svg>
    );
    case 'shield-check': return (
      <svg {...props}><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3z"/><path d="M9 12l2 2 4-4"/></svg>
    );
    case 'edit': return (
      <svg {...props}><path d="M4 20h4l11-11-4-4L4 16v4z"/><path d="M14 6l4 4"/></svg>
    );
    case 'trash': return (
      <svg {...props}><path d="M4 7h16M9 7V4h6v3M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13"/></svg>
    );
    case 'sync': return (
      <svg {...props}><path d="M21 12a9 9 0 0 1-15 6.7L3 16M3 12a9 9 0 0 1 15-6.7L21 8"/><path d="M21 3v5h-5M3 21v-5h5"/></svg>
    );
    case 'wifi-off': return (
      <svg {...props}><path d="M3 3l18 18"/><path d="M9 17a4 4 0 0 1 6 0M5 13a10 10 0 0 1 4-2.7M19 13a10 10 0 0 0-3-2.2M2 9a17 17 0 0 1 5-2.8M22 9a17 17 0 0 0-9-3"/><circle cx="12" cy="20" r="0.8" fill={stroke}/></svg>
    );
    case 'cloud-up': return (
      <svg {...props}><path d="M7 18A4 4 0 0 1 7 10a6 6 0 0 1 11-1 4 4 0 0 1-1 8h-2"/><path d="M12 12v8M9 15l3-3 3 3"/></svg>
    );
    case 'cloud': return (
      <svg {...props}><path d="M7 18A4 4 0 0 1 7 10a6 6 0 0 1 11-1 4 4 0 0 1-1 8H7z"/></svg>
    );
    case 'database': return (
      <svg {...props}><ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v6c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 11v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6"/></svg>
    );
    case 'document': return (
      <svg {...props}><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M14 3v6h6M8 13h8M8 17h6"/></svg>
    );
    case 'camera': return (
      <svg {...props}><path d="M5 8h2l2-3h6l2 3h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z"/><circle cx="12" cy="13" r="4"/></svg>
    );
    case 'image': return (
      <svg {...props}><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="M3 18l5-5 4 4 3-3 6 6"/></svg>
    );
    case 'flip': return (
      <svg {...props}><path d="M21 12a9 9 0 0 1-15 6.7L3 16M3 12a9 9 0 0 1 15-6.7L21 8"/></svg>
    );
    case 'flash': return (<svg {...props}><path d="M13 2L4 14h7l-1 8 9-12h-7l1-8z"/></svg>);
    case 'star': return (filled ? (
      <svg {...props} fill={stroke} stroke="none"><path d="M12 3l2.7 5.5 6 .9-4.4 4.2 1 6-5.3-2.8-5.3 2.8 1-6L4 9.4l6-.9z"/></svg>
    ) : (
      <svg {...props}><path d="M12 3l2.7 5.5 6 .9-4.4 4.2 1 6-5.3-2.8-5.3 2.8 1-6L4 9.4l6-.9z"/></svg>
    ));
    case 'archive': return (
      <svg {...props}><rect x="3" y="4" width="18" height="4" rx="1"/><path d="M5 8v11a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8M9 12h6"/></svg>
    );
    case 'filter': return (<svg {...props}><path d="M3 5h18l-7 9v6l-4-2v-4z"/></svg>);
    case 'info': return (<svg {...props}><circle cx="12" cy="12" r="9"/><path d="M12 8h.01M11 12h1v5h1"/></svg>);
    case 'warning': return (
      <svg {...props}><path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18h.01"/></svg>
    );
    case 'help': return (<svg {...props}><circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 1 1 4 2L12 13M12 17h.01"/></svg>);
    case 'logout': return (
      <svg {...props}><path d="M9 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M16 8l4 4-4 4M20 12H10"/></svg>
    );
    case 'login': return (<svg {...props}><path d="M15 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4M10 8l-4 4 4 4M6 12h12"/></svg>);
    case 'share': return (
      <svg {...props}><circle cx="6" cy="12" r="2"/><circle cx="18" cy="6" r="2"/><circle cx="18" cy="18" r="2"/><path d="M8 11l8-4M8 13l8 4"/></svg>
    );
    case 'sun': return (
      <svg {...props}><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>
    );
    case 'moon': return (<svg {...props}><path d="M21 13A9 9 0 1 1 11 3a7 7 0 0 0 10 10z"/></svg>);
    case 'gauge': return (
      <svg {...props}><path d="M3 13a9 9 0 0 1 18 0"/><path d="M12 13l4-3"/><circle cx="12" cy="13" r="1.2" fill={stroke}/></svg>
    );
    case 'clipboard': return (
      <svg {...props}><rect x="6" y="4" width="12" height="17" rx="2"/><path d="M9 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1"/><path d="M9 11h6M9 15h4"/></svg>
    );
    case 'flame': return (<svg {...props}><path d="M12 2s4 4 4 9a4 4 0 1 1-8 0c0-2 1-3 1-3s2 1 2 4c0-3 1-7 1-10z"/></svg>);
    case 'water': return (<svg {...props}><path d="M12 3s7 8 7 13a7 7 0 1 1-14 0c0-5 7-13 7-13z"/></svg>);
    case 'tag': return (<svg {...props}><path d="M3 12V5a2 2 0 0 1 2-2h7l9 9-9 9-9-9z"/><circle cx="8" cy="8" r="1.2" fill={stroke}/></svg>);
    case 'badge': return (<svg {...props}><path d="M12 3l9 4-1 12-8 2-8-2L3 7z"/><path d="M9 12l2 2 4-4"/></svg>);
    case 'building': return (
      <svg {...props}><rect x="4" y="3" width="16" height="18" rx="1"/><path d="M8 7h2M8 11h2M8 15h2M14 7h2M14 11h2M14 15h2M10 21v-3h4v3"/></svg>
    );
    case 'users': return (
      <svg {...props}><circle cx="9" cy="9" r="3.5"/><path d="M2 20a7 7 0 0 1 14 0"/><path d="M16 4a3.5 3.5 0 0 1 0 7M22 20a6 6 0 0 0-5-6"/></svg>
    );
    case 'reject': return (<svg {...props}><circle cx="12" cy="12" r="9"/><path d="M8 8l8 8M16 8l-8 8"/></svg>);
    case 'gavel': return (<svg {...props}><path d="M9 5l5 5M5 9l5 5M3 21h12M11 13l-7 7"/></svg>);
    case 'kpi-up': return (<svg {...props}><path d="M3 17l6-6 4 4 8-9"/><path d="M14 6h7v7"/></svg>);
    case 'pin': return (<svg {...props}><path d="M12 2v6M9 8h6l-1 6h-4z M12 14v8"/></svg>);
    case 'sliders': return (
      <svg {...props}><path d="M4 6h7M14 6h6M4 12h3M10 12h10M4 18h12M19 18h1"/><circle cx="12" cy="6" r="2"/><circle cx="8" cy="12" r="2"/><circle cx="17" cy="18" r="2"/></svg>
    );
    case 'leaf-fill': return (
      <svg {...props} fill={stroke} stroke="none">
        <path d="M21 3c0 9-6 16-14 18 0-9 6-16 14-18z" opacity="0.9"/>
      </svg>
    );
    default: return (<svg {...props}><circle cx="12" cy="12" r="9"/></svg>);
  }
}

window.Icon = Icon;
