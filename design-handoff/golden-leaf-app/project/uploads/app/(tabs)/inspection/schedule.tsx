import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Alert, Platform, Pressable, ScrollView, StyleSheet, View } from "react-native";
import DateTimePicker from "@react-native-community/datetimepicker";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";
import { FullScreenLeafLoader, LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { deleteScheduledInspection, saveScheduledInspection } from "@/lib/inspection-storage";
import { readCacheItems } from "@/lib/offline-cache";

const API_BASE_URL_KEY = "tbz:portalBaseUrl:v1";
const AUTH_ACCESS_KEY = "tbz:portalAccessToken:v1";
const AUTH_REFRESH_KEY = "tbz:portalRefreshToken:v1";

function normalizeBaseUrl(raw: string) {
  return raw.trim().replace(/\/+$/, "");
}

function sanitizePortalBaseUrl(raw: string) {
  const value = normalizeBaseUrl(raw);
  if (!value) return "";
  try {
    const url = new URL(value);
    return normalizeBaseUrl(`${url.protocol}//${url.host}`);
  } catch {
    try {
      const needsHttp =
        value.startsWith("localhost") ||
        value.startsWith("127.0.0.1") ||
        /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(value);
      const url = new URL(`${needsHttp ? "http" : "https"}://${value}`);
      return normalizeBaseUrl(`${url.protocol}//${url.host}`);
    } catch {
      return value;
    }
  }
}

function runtimePortalBaseUrl(raw: string) {
  const baseUrl = sanitizePortalBaseUrl(raw);
  if (!baseUrl) return "";
  try {
    const url = new URL(baseUrl);
    const host = url.hostname;
    if (Platform.OS === "android" && (host === "localhost" || host === "127.0.0.1")) {
      url.hostname = "10.0.2.2";
      return normalizeBaseUrl(url.toString());
    }
    return normalizeBaseUrl(url.toString());
  } catch {
    return baseUrl;
  }
}

type PortalUser = {
  id: string;
  full_name?: string;
  first_name?: string;
  last_name?: string;
  email?: string;
  roles?: string[];
  is_active?: boolean;
};

type PortalGrower = {
  id: string;
  display_name?: string;
  tbz_id?: string | null;
  nrc_number?: string;
  province?: string;
  district?: string;
};

type ApiListResponse<T> = {
  results?: T[];
} | T[];

function inspectionTypeCode(label: string) {
  if (label === "Grower Validation") return "GROWER_VALIDATION";
  if (label === "Nursery Inspection") return "NURSERY_INSPECTION";
  if (label === "Field Inspection") return "FIELD_INSPECTION";
  if (label === "Curing Inspection") return "CURING_INSPECTION";
  return "";
}

function provinceCode(label: string) {
  const up = label.trim().toUpperCase().replace(/[-\s]/g, "_");
  if (up === "NORTHWESTERN") return "NORTH_WESTERN";
  return up;
}

const INSPECTION_TYPE_OPTIONS = [
  "Grower Validation",
  "Nursery Inspection",
  "Field Inspection",
  "Curing Inspection",
];

const PROVINCE_OPTIONS = [
  "Central",
  "Copperbelt",
  "Eastern",
  "Luapula",
  "Lusaka",
  "Muchinga",
  "Northern",
  "North-Western",
  "Southern",
  "Western",
];

const DISTRICTS_BY_PROVINCE: Record<string, string[]> = {
  CENTRAL: [
    "Chibombo",
    "Chisamba",
    "Chitambo",
    "Kabwe",
    "Kapiri Mposhi",
    "Luano",
    "Mkushi",
    "Mumbwa",
    "Ngabwe",
    "Serenje",
    "Shibuyunji",
  ],
  COPPERBELT: [
    "Chililabombwe",
    "Chingola",
    "Kalulushi",
    "Kitwe",
    "Luanshya",
    "Lufwanyama",
    "Masaiti",
    "Mpongwe",
    "Mufulira",
    "Ndola",
  ],
  EASTERN: [
    "Chadiza",
    "Chama",
    "Chasefu",
    "Chipangali",
    "Chipata",
    "Kasenengwa",
    "Katete",
    "Lumezi",
    "Lundazi",
    "Lusangazi",
    "Mambwe",
    "Nyimba",
    "Petauke",
    "Sinda",
    "Vubwi",
  ],
  LUAPULA: [
    "Chembe",
    "Chiengi",
    "Chifunabuli",
    "Chipili",
    "Kawambwa",
    "Lunga",
    "Mansa",
    "Milenge",
    "Mwansabombwe",
    "Mwense",
    "Nchelenge",
    "Samfya",
  ],
  LUSAKA: ["Chilanga", "Chongwe", "Kafue", "Luangwa", "Lusaka", "Rufunsa"],
  MUCHINGA: [
    "Chinsali",
    "Isoka",
    "Kanchibiya",
    "Lavushimanda",
    "Mafinga",
    "Mpika",
    "Nakonde",
    "Shiwang'andu",
  ],
  NORTHERN: [
    "Chilubi",
    "Kaputa",
    "Kasama",
    "Lunte",
    "Lupososhi",
    "Luwingu",
    "Mbala",
    "Mporokoso",
    "Mpulungu",
    "Mungwi",
    "Nsama",
    "Senga",
  ],
  NORTH_WESTERN: [
    "Chavuma",
    "Ikelenge",
    "Kabompo",
    "Kasempa",
    "Kalumbila",
    "Manyinga",
    "Mufumbwe",
    "Mushindamo",
    "Mwinilunga",
    "Solwezi",
    "Zambezi",
  ],
  SOUTHERN: [
    "Chikankata",
    "Chirundu",
    "Choma",
    "Gwembe",
    "Itezhi-Tezhi",
    "Kalomo",
    "Kazungula",
    "Livingstone",
    "Mazabuka",
    "Monze",
    "Namwala",
    "Pemba",
    "Siavonga",
    "Sinazongwe",
    "Zimba",
  ],
  WESTERN: [
    "Kalabo",
    "Kaoma",
    "Limulunga",
    "Luampa",
    "Lukulu",
    "Mitete",
    "Mongu",
    "Mulobezi",
    "Mwandi",
    "Nalolo",
    "Nkeyema",
    "Senanga",
    "Sesheke",
    "Shang'ombo",
    "Sikongo",
    "Sioma",
  ],
};

function listFromApi<T>(payload: ApiListResponse<T>) {
  return Array.isArray(payload) ? payload : payload.results ?? [];
}

function inspectionTypeLabelFromCode(code: string) {
  const c = (code ?? "").trim().toUpperCase();
  if (c === "GROWER_VALIDATION") return "Grower Validation";
  if (c === "NURSERY_INSPECTION") return "Nursery Inspection";
  if (c === "FIELD_INSPECTION") return "Field Inspection";
  if (c === "CURING_INSPECTION") return "Curing Inspection";
  return "Grower Validation";
}

function provinceLabelFromCode(code: string) {
  const c = (code ?? "").trim().toUpperCase();
  return PROVINCE_OPTIONS.find((opt) => provinceCode(opt) === c) ?? c;
}

function parseYmdLocal(ymd: string) {
  const raw = (ymd ?? "").trim();
  const [y, m, d] = raw.split("-").map((x) => Number(x));
  if (!y || !m || !d) return new Date();
  return new Date(y, m - 1, d);
}

function formatYmdLocal(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export default function InspectionScheduleScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [portalBaseUrl, setPortalBaseUrl] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [refreshToken, setRefreshToken] = useState("");

  const [growerSearch, setGrowerSearch] = useState("");
  const [growerOptions, setGrowerOptions] = useState<PortalGrower[]>([]);
  const [growerLoading, setGrowerLoading] = useState(false);
  const [selectedGrower, setSelectedGrower] = useState<PortalGrower | null>(null);

  const [inspectorSearch, setInspectorSearch] = useState("");
  const [inspectorOptions, setInspectorOptions] = useState<PortalUser[]>([]);
  const [inspectorLoading, setInspectorLoading] = useState(false);
  const [selectedInspector, setSelectedInspector] = useState<PortalUser | null>(null);

  const [inspectionTypeLabel, setInspectionTypeLabel] = useState("Grower Validation");
  
  const [scheduledDate, setScheduledDate] = useState(new Date());
  
  const [showDatePicker, setShowDatePicker] = useState(false);

  const [provinceLabel, setProvinceLabel] = useState("");
  const [districtLabel, setDistrictLabel] = useState("");
  const [districtOptions, setDistrictOptions] = useState<string[]>([]);
  const [districtLoading, setDistrictLoading] = useState(false);
  const [notes, setNotes] = useState("");

  const lastPrefilledDraftIdRef = useRef<string | null>(null);
  const growerDetailsLoadedForDraftIdRef = useRef<string | null>(null);
  const inspectorDetailsLoadedForDraftIdRef = useRef<string | null>(null);
  const districtLabelRef = useRef("");

  useEffect(() => {
    districtLabelRef.current = districtLabel;
  }, [districtLabel]);

  const draftIdParam = params.draftId;
  const draftId = Array.isArray(draftIdParam) ? draftIdParam[0] : (draftIdParam ?? "");
  const isEditing = Boolean(String(draftId).trim());
  const returnToParam = params.returnTo;
  const returnTo = Array.isArray(returnToParam) ? returnToParam[0] : (returnToParam ?? "");

  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);

  const canSubmit = useMemo(() => {
    return (
      Boolean((selectedGrower?.id ?? "").trim() || growerSearch.trim()) &&
      Boolean((selectedInspector?.id ?? "").trim() || inspectorSearch.trim()) &&
      Boolean(provinceLabel.trim()) &&
      Boolean(districtLabel.trim())
    );
  }, [districtLabel, growerSearch, inspectorSearch, provinceLabel, selectedGrower?.id, selectedInspector?.id]);

  const refreshAccessToken = useCallback(async (baseUrl: string, token: string) => {
    const resp = await fetch(`${runtimePortalBaseUrl(baseUrl)}/api/v1/auth/token/refresh/`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh: token }),
    });
    if (!resp.ok) return null;
    const payload = (await resp.json()) as { access?: string };
    const next = payload.access ?? "";
    return next || null;
  }, []);

  const apiFetch = useCallback(async (path: string, init: RequestInit = {}) => {
    const baseUrl = runtimePortalBaseUrl(portalBaseUrl);
    const url = `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
    const headers = new Headers(init.headers);
    if (!headers.has("Content-Type") && init.body && !(init.body instanceof FormData)) {
      headers.set("Content-Type", "application/json");
    }
    if (!headers.has("Accept")) headers.set("Accept", "application/json");
    if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

    const first = await fetch(url, { ...init, headers });
    if (first.status !== 401) return first;
    if (!refreshToken) return first;

    const nextAccess = await refreshAccessToken(portalBaseUrl, refreshToken);
    if (!nextAccess) return first;
    setAccessToken(nextAccess);
    await AsyncStorage.setItem(AUTH_ACCESS_KEY, nextAccess);
    headers.set("Authorization", `Bearer ${nextAccess}`);
    return fetch(url, { ...init, headers });
  }, [accessToken, portalBaseUrl, refreshAccessToken, refreshToken]);

  const requirePortalSession = useCallback(async () => {
    const savedBase = await AsyncStorage.getItem(API_BASE_URL_KEY);
    const savedAccess = await AsyncStorage.getItem(AUTH_ACCESS_KEY);
    const savedRefresh = await AsyncStorage.getItem(AUTH_REFRESH_KEY);
    const baseUrl = sanitizePortalBaseUrl(savedBase ?? "");
    if (!baseUrl || !savedAccess || !savedRefresh) {
      router.push({
        pathname: "/portal-login",
        params: { returnTo: returnTo || "/inspection/schedule" },
      } as any);
      return false;
    }
    setPortalBaseUrl(baseUrl);
    setAccessToken(savedAccess);
    setRefreshToken(savedRefresh);
    return true;
  }, [returnTo, router]);

  useEffect(() => {
    (async () => {
      setBooting(true);
      const ok = await requirePortalSession();
      setBooting(false);
      if (!ok) return;
    })();
  }, [requirePortalSession]);

  // Prefill fields when navigated from a specific offline schedule card.
  useEffect(() => {
    if (!isEditing) return;
    const dId = String(draftId ?? "").trim();
    if (!dId) return;
    if (lastPrefilledDraftIdRef.current === dId) return;

    const getStr = (v: unknown) => (Array.isArray(v) ? v[0] : v ?? "");
    const growerId = String(getStr(params.growerId)).trim();
    const inspectorId = String(getStr(params.inspectorId)).trim();

    const inspectionTypeCodeValue = String(getStr(params.inspectionType)).trim();

    setSelectedGrower({
      id: growerId,
      display_name: "Selected grower",
      tbz_id: "",
      province: "",
      district: "",
    } as any);

    setInspectionTypeLabel(inspectionTypeLabelFromCode(inspectionTypeCodeValue));
    setScheduledDate(parseYmdLocal(String(getStr(params.scheduledDate))));

    setProvinceLabel(provinceLabelFromCode(String(getStr(params.province))));
    setDistrictLabel(String(getStr(params.district) ?? ""));
    setNotes(String(getStr(params.notes) ?? ""));

    setSelectedInspector(
      inspectorId
        ? ({
            id: inspectorId,
            full_name: "Selected inspector",
          } as any)
        : null,
    );
    lastPrefilledDraftIdRef.current = dId;
  }, [
    draftId,
    isEditing,
    params.district,
    params.growerId,
    params.inspectionType,
    params.inspectorId,
    params.notes,
    params.province,
    params.scheduledDate,
  ]);

  // Fetch the real grower details by id once the portal session is ready.
  useEffect(() => {
    if (!isEditing) return;
    const dId = String(draftId ?? "").trim();
    if (!dId) return;
    if (!portalBaseUrl || !accessToken) return;
    if (growerDetailsLoadedForDraftIdRef.current === dId) return;

    const getStr = (v: unknown) => (Array.isArray(v) ? v[0] : v ?? "");
    const growerId = String(getStr(params.growerId)).trim();
    if (!growerId) return;

    void (async () => {
      try {
        const resp = await apiFetch(`/api/v1/growers/growers/${encodeURIComponent(growerId)}/`, {
          method: "GET",
        });
        if (!resp.ok) return;
        const data = (await resp.json()) as any;
        const fullName = [data?.first_name, data?.middle_name, data?.last_name]
          .filter(Boolean)
          .join(" ")
          .trim();
        setSelectedGrower({
          id: String(data?.id ?? growerId),
          display_name: data?.display_name ?? (fullName || "Selected grower"),
          tbz_id: String(data?.tbz_id ?? ""),
          nrc_number: String(data?.nrc_number ?? ""),
          province: String(data?.province ?? ""),
          district: String(data?.district ?? ""),
        } as any);
        growerDetailsLoadedForDraftIdRef.current = dId;
      } catch {
        // If it fails, keep placeholder; user can re-search grower manually.
      }
    })();
  }, [isEditing, draftId, portalBaseUrl, accessToken, params.growerId, apiFetch]);

  // Fetch the real inspector details by id once the portal session is ready.
  useEffect(() => {
    if (!isEditing) return;
    const dId = String(draftId ?? "").trim();
    if (!dId) return;
    if (!portalBaseUrl || !accessToken) return;
    if (inspectorDetailsLoadedForDraftIdRef.current === dId) return;

    const getStr = (v: unknown) => (Array.isArray(v) ? v[0] : v ?? "");
    const inspectorId = String(getStr(params.inspectorId)).trim();
    if (!inspectorId) return;

    void (async () => {
      try {
        const resp = await apiFetch(`/api/v1/auth/users/${encodeURIComponent(inspectorId)}/`, { method: "GET" });
        if (!resp.ok) return;
        const data = (await resp.json()) as any;
        setSelectedInspector({
          id: String(data?.id ?? inspectorId),
          full_name: String(data?.full_name ?? "").trim() || "Selected inspector",
          email: String(data?.email ?? ""),
          roles: Array.isArray(data?.roles) ? data.roles : [],
          is_active: Boolean(data?.is_active),
        } as any);
        inspectorDetailsLoadedForDraftIdRef.current = dId;
      } catch {
      }
    })();
  }, [accessToken, apiFetch, draftId, isEditing, params.inspectorId, portalBaseUrl]);

  useEffect(() => {
    if (!portalBaseUrl || !accessToken) return;
    const province = provinceCode(provinceLabel);
    if (!province) {
      setDistrictOptions([]);
      setDistrictLoading(false);
      return;
    }
    setDistrictOptions([]);
    void (async () => {
      setDistrictLoading(true);
      try {
        let list: string[] = [];

        const ajaxResp = await apiFetch(`/growers/ajax/districts/?province=${encodeURIComponent(province)}`, { method: "GET" });
        if (ajaxResp.status === 401) {
          router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedule" } } as any);
          return;
        }

        const ajaxContentType = (ajaxResp.headers.get("content-type") ?? "").toLowerCase();
        if (ajaxResp.ok && ajaxContentType.includes("application/json")) {
          const payload = (await ajaxResp.json()) as { districts?: { id: string; name: string }[] };
          list = (payload?.districts ?? []).map((d) => d.name).filter(Boolean);
        } else {
          list = DISTRICTS_BY_PROVINCE[province] ?? [];

          if (list.length === 0) {
            const districts: string[] = [];
            let url = `/api/v1/growers/growers/?province=${encodeURIComponent(province)}&ordering=district&page_size=500`;
            for (let page = 0; page < 10 && url; page += 1) {
              const resp = await apiFetch(url, { method: "GET" });
              if (resp.status === 401) {
                router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedule" } } as any);
                return;
              }
              if (!resp.ok) break;
              const payload = (await resp.json()) as any;
              const rows = Array.isArray(payload) ? payload : Array.isArray(payload?.results) ? payload.results : [];
              for (const row of rows) {
                const d = String(row?.district ?? "").trim();
                if (d && !districts.includes(d)) districts.push(d);
              }
              const next = typeof payload?.next === "string" ? payload.next : "";
              if (next) {
                try {
                  const u = new URL(next);
                  url = `${u.pathname}${u.search}`;
                } catch {
                  url = "";
                }
              } else {
                url = "";
              }
            }
            list = districts;
          }
        }

        list = [...new Set(list)].sort((a, b) => a.localeCompare(b));

        const current = districtLabelRef.current.trim();
        if (current && !list.includes(current)) list = [current, ...list];
        setDistrictOptions(list);
      } catch {
      } finally {
        setDistrictLoading(false);
      }
    })();
  }, [accessToken, apiFetch, portalBaseUrl, provinceLabel, router]);

  useEffect(() => {
    if (!portalBaseUrl || !accessToken) return;
    const q = growerSearch.trim();
    if (selectedGrower?.id && q && q === (selectedGrower.display_name ?? "").trim()) return;
    if (q.length < 2) {
      setGrowerOptions([]);
      return;
    }
    setGrowerLoading(true);
    const handle = setTimeout(() => {
      void (async () => {
        try {
          const resp = await apiFetch(`/api/v1/growers/growers/?search=${encodeURIComponent(q)}&page_size=10`, { method: "GET" });
          if (resp.status === 401) {
            router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedule" } } as any);
            return;
          }
          if (!resp.ok) return;
          const payload = (await resp.json()) as ApiListResponse<PortalGrower>;
          const list = listFromApi(payload);
          setGrowerOptions(list);

          if (!selectedGrower && list.length === 1) {
            const only = list[0];
            const dn = String(only?.display_name ?? "").trim().toLowerCase();
            const tbz = String(only?.tbz_id ?? "").trim().toLowerCase();
            const nrc = String(only?.nrc_number ?? "").trim().toLowerCase().replace(/\s+/g, "");
            const needle = q.toLowerCase().trim();
            const needleCompact = needle.replace(/\s+/g, "");
            if (dn && dn === needle || tbz && tbz === needle || nrc && nrc === needleCompact) {
              setSelectedGrower(only);
              setGrowerOptions([]);
              setGrowerSearch(String(only.display_name ?? ""));
              setProvinceLabel(provinceLabelFromCode(String(only.province ?? "")));
              setDistrictLabel(String(only.district ?? ""));
            }
          }
        } catch {
          try {
            const cached = await readCacheItems<any>("growers");
            const needle = q.toLowerCase().trim();
            const needleCompact = needle.replace(/\s+/g, "");
            const matches = cached
              .filter((g) => {
                const dn = String(g?.display_name ?? "").trim().toLowerCase();
                const tbz = String(g?.tbz_id ?? "").trim().toLowerCase();
                const nrc = String(g?.nrc_number ?? "").trim().toLowerCase().replace(/\s+/g, "");
                const hay = `${dn} ${tbz} ${nrc}`.trim();
                return hay.includes(needle) || nrc.includes(needleCompact);
              })
              .slice(0, 10);
            setGrowerOptions(matches);

            if (!selectedGrower && matches.length === 1) {
              const only = matches[0];
              const dn = String(only?.display_name ?? "").trim().toLowerCase();
              const tbz = String(only?.tbz_id ?? "").trim().toLowerCase();
              const nrc = String(only?.nrc_number ?? "").trim().toLowerCase().replace(/\s+/g, "");
              if ((dn && dn === needle) || (tbz && tbz === needle) || (nrc && nrc === needleCompact)) {
                setSelectedGrower(only);
                setGrowerOptions([]);
                setGrowerSearch(String(only.display_name ?? ""));
                setProvinceLabel(provinceLabelFromCode(String(only.province ?? "")));
                setDistrictLabel(String(only.district ?? ""));
              }
            }
          } catch {
          }
        } finally {
          setGrowerLoading(false);
        }
      })();
    }, 280);
    return () => clearTimeout(handle);
  }, [accessToken, apiFetch, growerSearch, portalBaseUrl, router, selectedGrower?.display_name, selectedGrower?.id]);

  useEffect(() => {
    if (!portalBaseUrl || !accessToken) return;
    const q = inspectorSearch.trim();
    if (selectedInspector?.id && q && q === (selectedInspector.full_name ?? "").trim()) return;
    if (q.length < 2) {
      setInspectorOptions([]);
      return;
    }
    setInspectorLoading(true);
    const handle = setTimeout(() => {
      void (async () => {
        try {
          const resp = await apiFetch(
            `/api/v1/auth/users/?role=INSPECTOR&search=${encodeURIComponent(q)}&page_size=10`,
            { method: "GET" },
          );
          if (resp.status === 401) {
            router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedule" } } as any);
            return;
          }
          if (!resp.ok) return;
          const payload = (await resp.json()) as ApiListResponse<PortalUser>;
          const list = listFromApi(payload);
          setInspectorOptions(list);

          if (!selectedInspector && list.length === 1) {
            const only = list[0];
            const fn = String(only?.full_name ?? "").trim().toLowerCase();
            const email = String(only?.email ?? "").trim().toLowerCase();
            const needle = q.toLowerCase().trim();
            if (fn && fn === needle || email && email === needle) {
              setSelectedInspector(only);
              setInspectorOptions([]);
              setInspectorSearch(String(only.full_name ?? ""));
            }
          }
        } catch {
          try {
            const cached = await readCacheItems<any>("inspectors");
            const needle = q.toLowerCase().trim();
            const matches = cached
              .filter((u) => {
                const fn = String(u?.full_name ?? "").trim().toLowerCase();
                const email = String(u?.email ?? "").trim().toLowerCase();
                const hay = `${fn} ${email}`.trim();
                return hay.includes(needle);
              })
              .slice(0, 10);
            setInspectorOptions(matches);

            if (!selectedInspector && matches.length === 1) {
              const only = matches[0];
              const fn = String(only?.full_name ?? "").trim().toLowerCase();
              const email = String(only?.email ?? "").trim().toLowerCase();
              if ((fn && fn === needle) || (email && email === needle)) {
                setSelectedInspector(only);
                setInspectorOptions([]);
                setInspectorSearch(String(only.full_name ?? ""));
              }
            }
          } catch {
          }
        } finally {
          setInspectorLoading(false);
        }
      })();
    }, 280);
    return () => clearTimeout(handle);
  }, [accessToken, apiFetch, inspectorSearch, portalBaseUrl, router, selectedInspector?.full_name, selectedInspector?.id]);

  async function scheduleInspection() {
    let grower = selectedGrower;
    if (!grower?.id) {
      const q = growerSearch.trim();
      if (!q) {
        Alert.alert("Missing fields", "Select grower.");
        return;
      }
      try {
        const resp = await apiFetch(`/api/v1/growers/growers/?search=${encodeURIComponent(q)}&page_size=10`, { method: "GET" });
        if (resp.status === 401) {
          router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedule" } } as any);
          return;
        }
        if (resp.ok) {
          const payload = (await resp.json()) as ApiListResponse<PortalGrower>;
          const list = listFromApi(payload);
          const needle = q.toLowerCase().trim();
          const needleCompact = needle.replace(/\s+/g, "");
          const exact = list.filter((g) => {
            const dn = String(g?.display_name ?? "").trim().toLowerCase();
            const tbz = String(g?.tbz_id ?? "").trim().toLowerCase();
            const nrc = String(g?.nrc_number ?? "").trim().toLowerCase().replace(/\s+/g, "");
            return (dn && dn === needle) || (tbz && tbz === needle) || (nrc && nrc === needleCompact);
          });
          const chosen = exact.length === 1 ? exact[0] : list.length === 1 ? list[0] : null;
          if (chosen?.id) {
            grower = chosen;
            setSelectedGrower(chosen);
            setGrowerOptions([]);
            setGrowerSearch(String(chosen.display_name ?? q));
            if (!provinceLabel.trim()) setProvinceLabel(provinceLabelFromCode(String(chosen.province ?? "")));
            if (!districtLabel.trim()) setDistrictLabel(String(chosen.district ?? ""));
          }
        }
      } catch {
        try {
          const cached = await readCacheItems<any>("growers");
          const needle = q.toLowerCase().trim();
          const needleCompact = needle.replace(/\s+/g, "");
          const matches = cached.filter((g) => {
            const dn = String(g?.display_name ?? "").trim().toLowerCase();
            const tbz = String(g?.tbz_id ?? "").trim().toLowerCase();
            const nrc = String(g?.nrc_number ?? "").trim().toLowerCase().replace(/\s+/g, "");
            return (dn && dn === needle) || (tbz && tbz === needle) || (nrc && nrc === needleCompact);
          });
          const chosen = matches.length === 1 ? matches[0] : null;
          if (chosen?.id) {
            grower = chosen;
            setSelectedGrower(chosen);
            setGrowerOptions([]);
            setGrowerSearch(String(chosen.display_name ?? q));
            if (!provinceLabel.trim()) setProvinceLabel(provinceLabelFromCode(String(chosen.province ?? "")));
            if (!districtLabel.trim()) setDistrictLabel(String(chosen.district ?? ""));
          }
        } catch {
        }
      }
    }
    if (!grower?.id) {
      Alert.alert("Missing fields", "Select grower from the results list.");
      return;
    }

    let inspector = selectedInspector;
    if (!inspector?.id) {
      const q = inspectorSearch.trim();
      if (!q) {
        Alert.alert("Missing fields", "Assign inspector.");
        return;
      }
      try {
        const resp = await apiFetch(`/api/v1/auth/users/?role=INSPECTOR&search=${encodeURIComponent(q)}&page_size=10`, { method: "GET" });
        if (resp.status === 401) {
          router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedule" } } as any);
          return;
        }
        if (resp.ok) {
          const payload = (await resp.json()) as ApiListResponse<PortalUser>;
          const list = listFromApi(payload);
          const needle = q.toLowerCase().trim();
          const exact = list.filter((u) => {
            const fn = String(u?.full_name ?? "").trim().toLowerCase();
            const email = String(u?.email ?? "").trim().toLowerCase();
            return (fn && fn === needle) || (email && email === needle);
          });
          const chosen = exact.length === 1 ? exact[0] : list.length === 1 ? list[0] : null;
          if (chosen?.id) {
            inspector = chosen;
            setSelectedInspector(chosen);
            setInspectorOptions([]);
            setInspectorSearch(String(chosen.full_name ?? q));
          }
        }
      } catch {
        try {
          const cached = await readCacheItems<any>("inspectors");
          const needle = q.toLowerCase().trim();
          const matches = cached.filter((u) => {
            const fn = String(u?.full_name ?? "").trim().toLowerCase();
            const email = String(u?.email ?? "").trim().toLowerCase();
            return (fn && fn === needle) || (email && email === needle);
          });
          const chosen = matches.length === 1 ? matches[0] : null;
          if (chosen?.id) {
            inspector = chosen;
            setSelectedInspector(chosen);
            setInspectorOptions([]);
            setInspectorSearch(String(chosen.full_name ?? q));
          }
        } catch {
        }
      }
    }
    if (!inspector?.id) {
      Alert.alert("Missing fields", "Select inspector from the results list.");
      return;
    }

    if (!provinceLabel) {
      Alert.alert("Missing fields", "Select province.");
      return;
    }
    if (!districtLabel) {
      Alert.alert("Missing fields", "Select district.");
      return;
    }

    setLoading(true);
    try {
      if (isEditing && String(draftId).trim()) {
        // Replace the existing offline draft with updated values.
        await deleteScheduledInspection(String(draftId).trim());
      }
      await saveScheduledInspection({
        payload: {
          growerId: grower.id,
          inspectorId: inspector.id,
          inspectionType: inspectionTypeCode(inspectionTypeLabel),
          scheduledDate: formatYmdLocal(scheduledDate),
          province: provinceCode(provinceLabel),
          district: districtLabel,
          notes,
        }
      });
      
      Alert.alert("Saved offline", "Inspection has been scheduled and saved offline. It will synchronize automatically when connection is restored.", [
        {
          text: "OK",
          onPress: () => router.replace((returnTo || "/inspection") as any),
        },
      ]);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to save offline";
      Alert.alert("Save failed", msg);
    } finally {
      setLoading(false);
    }
  }

  if (booting) {
    return <FullScreenLeafLoader label="Loading…" />;
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.headerRow}>
          <View style={styles.headerLeft}>
            <MaterialIcons name="event-available" size={22} color={Colors[theme].primary} />
          </View>
        </View>

        <View style={styles.section}>
          <TextField
            label="Grower"
            value={growerSearch}
            onChangeText={(v) => {
              setGrowerSearch(v);
              if (selectedGrower) setSelectedGrower(null);
            }}
            placeholder="Search by name, TBZ ID, or NRC..."
            required
          />
          {growerLoading ? <LeafLoader size={22} /> : null}
          {selectedGrower ? (
            <View style={[styles.selectedCard, { backgroundColor: Colors[theme].surface }]}>
              <ThemedText type="defaultSemiBold">{selectedGrower.display_name ?? "Selected grower"}</ThemedText>
              <ThemedText>
                {selectedGrower.tbz_id ?? "-"} • {selectedGrower.nrc_number ?? "-"}
              </ThemedText>
              <ThemedText>
                {selectedGrower.province ?? "-"} / {selectedGrower.district ?? "-"}
              </ThemedText>
              <Pressable
                onPress={() => {
                  setSelectedGrower(null);
                  setGrowerSearch("");
                }}
                style={styles.clearButton}
              >
                <ThemedText type="link">Clear selection</ThemedText>
              </Pressable>
            </View>
          ) : null}

          {growerOptions.length > 0 && !selectedGrower ? (
            <View style={[styles.resultsCard, { backgroundColor: Colors[theme].surface }]}>
              {growerOptions.map((item, index) => (
                <Pressable
                  key={item.id}
                  onPress={() => {
                    setSelectedGrower(item);
                    setGrowerOptions([]);
                    setGrowerSearch(item.display_name ?? "");
                    setProvinceLabel(provinceLabelFromCode(String(item.province ?? "")));
                    setDistrictLabel(String(item.district ?? ""));
                  }}
                  style={[
                    styles.resultRow,
                    { borderBottomColor: Colors[theme].border, borderBottomWidth: index === growerOptions.length - 1 ? 0 : 1 }
                  ]}
                >
                  <ThemedText type="defaultSemiBold">{item.display_name ?? "Grower"}</ThemedText>
                  <ThemedText>
                    {item.tbz_id ?? "-"} • {item.nrc_number ?? "-"}
                  </ThemedText>
                </Pressable>
              ))}
            </View>
          ) : null}
        </View>

        <View style={styles.section}>
          <TextField
            label="Assign Inspector"
            value={inspectorSearch}
            onChangeText={(v) => {
              setInspectorSearch(v);
              if (selectedInspector) setSelectedInspector(null);
            }}
            placeholder="Search inspector..."
            required
          />
          {inspectorLoading ? <LeafLoader size={22} /> : null}

          {selectedInspector ? (
            <View style={[styles.selectedCard, { backgroundColor: Colors[theme].surface }]}>
              <ThemedText type="defaultSemiBold">{selectedInspector.full_name ?? "Selected inspector"}</ThemedText>
              <ThemedText>{selectedInspector.email ?? "-"}</ThemedText>
              <Pressable
                onPress={() => {
                  setSelectedInspector(null);
                  setInspectorSearch("");
                }}
                style={styles.clearButton}
              >
                <ThemedText type="link">Clear selection</ThemedText>
              </Pressable>
            </View>
          ) : null}

          {inspectorOptions.length > 0 && !selectedInspector ? (
            <View style={[styles.resultsCard, { backgroundColor: Colors[theme].surface }]}>
              {inspectorOptions.map((item, index) => (
                <Pressable
                  key={item.id}
                  onPress={() => {
                    setSelectedInspector(item);
                    setInspectorOptions([]);
                    setInspectorSearch(item.full_name ?? "");
                  }}
                  style={[
                    styles.resultRow,
                    { borderBottomColor: Colors[theme].border, borderBottomWidth: index === inspectorOptions.length - 1 ? 0 : 1 }
                  ]}
                >
                  <ThemedText type="defaultSemiBold">{item.full_name ?? "Inspector"}</ThemedText>
                  <ThemedText>{item.email ?? "-"}</ThemedText>
                </Pressable>
              ))}
            </View>
          ) : null}
        </View>

        <View style={styles.section}>
          <SelectField label="Inspection Type" value={inspectionTypeLabel} onChange={setInspectionTypeLabel} options={INSPECTION_TYPE_OPTIONS} required />

          <ThemedText type="defaultSemiBold" style={styles.label}>Scheduled Date</ThemedText>
          <Pressable
            onPress={() => setShowDatePicker(true)}
            style={[styles.pickerButton, { backgroundColor: Colors[theme].background === '#152036' ? '#1E293B' : '#F3F4F6' }]}
          >
            <ThemedText>{formatYmdLocal(scheduledDate)}</ThemedText>
            <MaterialIcons name="calendar-today" size={20} color={Colors[theme].muted} />
          </Pressable>
          {showDatePicker && (
            <DateTimePicker
              value={scheduledDate}
              mode="date"
              display={Platform.OS === 'ios' ? 'spinner' : 'default'}
              onChange={(_event: any, selectedDate?: Date) => {
                setShowDatePicker(false);
                if (selectedDate) setScheduledDate(selectedDate);
              }}
            />
          )}

          <SelectField label="Province" value={provinceLabel} onChange={(v) => { setProvinceLabel(v); setDistrictLabel(""); }} options={PROVINCE_OPTIONS} required />
          {districtOptions.length > 0 ? (
            <SelectField
              label="District"
              value={districtLabel}
              onChange={setDistrictLabel}
              options={districtOptions}
              disabled={!provinceLabel || districtLoading}
              placeholder={!provinceLabel ? "Select province first…" : districtLoading ? "Loading…" : "Select…"}
              required
            />
          ) : (
            <TextField
              label="District"
              value={districtLabel}
              onChangeText={setDistrictLabel}
              placeholder={!provinceLabel ? "Select province first…" : districtLoading ? "Loading…" : "Type district…"}
              required
            />
          )}

          <TextField label="Notes" value={notes} onChangeText={setNotes} multiline />
        </View>

        <PrimaryButton title={loading ? "Scheduling..." : "Schedule"} onPress={scheduleInspection} disabled={loading || !canSubmit} />
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  loadingWrap: { flex: 1, alignItems: "center", justifyContent: "center", gap: 10 },
  content: { padding: 20, gap: 14, paddingBottom: 40 },
  headerRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  headerLeft: { flexDirection: "row", alignItems: "center", gap: 10 },
  section: { gap: 10 },
  searchRow: { 
    borderRadius: 16, 
    padding: 16, 
    gap: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
    marginBottom: 8,
    marginHorizontal: 4,
  },
  selectedCard: { 
    borderRadius: 16, 
    padding: 16, 
    gap: 8,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
    marginBottom: 8,
    marginHorizontal: 4,
  },
  resultsCard: { 
    borderRadius: 16, 
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
    marginHorizontal: 4,
  },
  resultRow: { paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1, gap: 4 },
  clearButton: { paddingTop: 8, alignSelf: "flex-start" },
  label: { fontSize: 13, marginBottom: 6, opacity: 0.8 },
  pickerButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    borderRadius: 16,
    paddingHorizontal: 16,
    height: 52,
  },
});
