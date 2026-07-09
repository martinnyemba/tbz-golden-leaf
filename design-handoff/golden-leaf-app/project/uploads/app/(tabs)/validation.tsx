import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import * as Location from "expo-location";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Platform, Pressable, ScrollView, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { getDeviceId, saveInspectionReport, type GrowerProfile, type GrowerValidation as GrowerValidationPayload, type InspectionReport } from "@/lib/inspection-storage";
import { readCacheItems, writeCache } from "@/lib/offline-cache";

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

type RefOption = { label: string; value: string };
type RefDistrict = { name: string; province: string; code?: string };

const SEX_OPTIONS: RefOption[] = [
  { label: "Male", value: "MALE" },
  { label: "Female", value: "FEMALE" },
];

const FALLBACK_CROP_STAGE_OPTIONS: RefOption[] = [
  { label: "Main Field", value: "MAIN_FIELD" },
  { label: "Topping", value: "TOPPING" },
  { label: "Reaping", value: "REAPING" },
  { label: "Curing", value: "CURING" },
  { label: "Grading", value: "GRADING" },
  { label: "Storage", value: "STORAGE" },
];

const FALLBACK_TOBACCO_TYPE_OPTIONS: RefOption[] = [
  { label: "Flue Cured Tobacco", value: "FLUE_CURED" },
  { label: "Burley", value: "BURLEY" },
  { label: "Dark Fired Tobacco", value: "DARK_FIRED" },
];

const FALLBACK_BARN_TYPE_OPTIONS: RefOption[] = [
  { label: "Flue Cured Barn", value: "FLUE_CURED" },
  { label: "Bulk Curer Barn", value: "BULK_CURE" },
  { label: "Air Cured Barn", value: "AIR_CURED" },
  { label: "Fire Cured Barn", value: "FIRE_CURED" },
  { label: "Conventional Barn", value: "CONVENTIONAL" },
  { label: "Traditional Barn", value: "TRADITIONAL" },
  { label: "Rocket Barn", value: "ROCKET" },
  { label: "Chongololo Barn", value: "CHONGOLOLO" },
  { label: "Matope Barn", value: "MATOPE" },
  { label: "Kamanga Barn", value: "KAMANGA" },
  { label: "Tunnel Barn", value: "TUNNEL" },
  { label: "Live Barn", value: "LIVE_BARN" },
];

function calculateYieldPerHa(hectarageRaw: string) {
  const n = Number(String(hectarageRaw ?? "").trim());
  if (!Number.isFinite(n) || n <= 0) return "";
  return n <= 9 ? "1500" : "3000";
}

function normalizeFromOptions(value: string, options: RefOption[]) {
  const v = String(value ?? "").trim();
  if (!v) return "";
  const direct = options.find((o) => o.value === v);
  if (direct) return direct.value;
  const byLabel = options.find((o) => o.label.toLowerCase() === v.toLowerCase());
  return byLabel ? byLabel.value : v;
}

function TogglePill({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.pill,
        {
          opacity: pressed ? 0.85 : 1,
          backgroundColor: selected ? "#0B6B3A" : "transparent",
          borderColor: selected ? "#0B6B3A" : "#E5E7EB",
        },
      ]}
    >
      <ThemedText lightColor={selected ? "#FFFFFF" : undefined} darkColor={selected ? "#FFFFFF" : undefined}>
        {label}
      </ThemedText>
    </Pressable>
  );
}

export default function ValidationScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const params = useLocalSearchParams<{ inspectionId?: string; grower?: string; growerId?: string; editingReportId?: string }>();
  const inspectionId = (params.inspectionId ?? "").toString();

  const initialGrower = useMemo(() => {
    try {
      return params.grower ? (JSON.parse(params.grower) as any) : null;
    } catch {
      return null;
    }
  }, [params.grower]);

  const [loading, setLoading] = useState(false);
  const [inspection, setInspection] = useState<any | null>(null);
  const [stakeholders, setStakeholders] = useState<{ id: string; name: string; code: string }[]>([]);
  const [sponsors, setSponsors] = useState<{ id: string; name: string; code: string }[]>([]);
  const [provinceOptions, setProvinceOptions] = useState<RefOption[]>([]);
  const [districts, setDistricts] = useState<RefDistrict[]>([]);
  const [districtOptions, setDistrictOptions] = useState<RefOption[]>([]);
  const [cropStageOptions, setCropStageOptions] = useState<RefOption[]>([]);
  const [tobaccoTypeOptions, setTobaccoTypeOptions] = useState<RefOption[]>([]);
  const [barnTypeOptions, setBarnTypeOptions] = useState<RefOption[]>([]);

  const [growerName, setGrowerName] = useState(initialGrower?.name ?? "");
  const [growerTbzId, setGrowerTbzId] = useState(initialGrower?.growerId ?? "");
  const [growerPortalId, setGrowerPortalId] = useState(initialGrower?.portalGrowerId ?? "");

  const [nrcNumber, setNrcNumber] = useState(initialGrower?.nrc ?? "");
  const [sex, setSex] = useState("");
  const [gpsLat, setGpsLat] = useState("");
  const [gpsLng, setGpsLng] = useState("");
  const [cropStage, setCropStage] = useState("");
  const [tobaccoType, setTobaccoType] = useState("");
  const [tobaccoVariety, setTobaccoVariety] = useState("");
  const [validatedHectarage, setValidatedHectarage] = useState("");
  const [yieldPerHa, setYieldPerHa] = useState("");
  const [sponsorId, setSponsorId] = useState("");
  const [barnType, setBarnType] = useState("");
  const [barnCount, setBarnCount] = useState("");
  const [capacityEnough, setCapacityEnough] = useState<"" | "Yes" | "No">("");
  const [stakeholderIds, setStakeholderIds] = useState<string[]>([]);
  const [province, setProvince] = useState(initialGrower?.province ?? "");
  const [district, setDistrict] = useState(initialGrower?.district ?? "");
  const [deviceId, setDeviceId] = useState("");
  const [remark, setRemark] = useState("");
  const [locationError, setLocationError] = useState("");

  useEffect(() => {
    if (!params.editingReportId) return;
    import("@/lib/inspection-storage").then((module) => {
      module.listInspectionReports().then((reports) => {
        const report = reports.find((r) => r.id === params.editingReportId);
        if (!report || report.stage !== "Grower Validation") return;
        const p = report.payload as GrowerValidationPayload;
        
        setNrcNumber(p.nrcNumber);
        setSex(p.sex);
        setGpsLat(p.gpsLatitude);
        setGpsLng(p.gpsLongitude);
        setCropStage(p.cropStage);
        setTobaccoType(p.tobaccoType);
        setTobaccoVariety(p.tobaccoVariety);
        setValidatedHectarage(p.validatedHectarage);
        setYieldPerHa(p.yieldPerHa);
        setSponsorId(p.sponsorId);
        setBarnType(p.barnType);
        setBarnCount(p.barnCount);
        setCapacityEnough(p.barnCapacitySufficient ? "Yes" : "No");
        setStakeholderIds(p.stakeholderIds || []);
        setProvince(p.province);
        setDistrict(p.district);
        setDeviceId(p.deviceId);
        setRemark(p.inspectorRemarks);
      });
    });
  }, [params.editingReportId]);

  const sponsorOptions = useMemo<RefOption[]>(() => sponsors.map((s) => ({ label: s.name, value: s.id })), [sponsors]);
  const sponsorName = useMemo(() => sponsors.find((s) => s.id === sponsorId)?.name ?? "", [sponsorId, sponsors]);
  const provinceLabel = useMemo(
    () => provinceOptions.find((p) => p.value === province)?.label ?? province,
    [province, provinceOptions],
  );

  const loadSession = useCallback(async () => {
    const savedBase = (await AsyncStorage.getItem(API_BASE_URL_KEY)) ?? "";
    const savedAccess = (await AsyncStorage.getItem(AUTH_ACCESS_KEY)) ?? "";
    const savedRefresh = (await AsyncStorage.getItem(AUTH_REFRESH_KEY)) ?? "";
    const baseUrl = runtimePortalBaseUrl(savedBase);
    if (!baseUrl || !savedAccess || !savedRefresh) return null;
    return { baseUrl, access: savedAccess, refresh: savedRefresh };
  }, []);

  const refreshAccess = useCallback(async (baseUrl: string, refresh: string) => {
    const resp = await fetch(`${baseUrl}/api/v1/auth/token/refresh/`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh }),
    });
    if (!resp.ok) return "";
    const payload = (await resp.json()) as { access?: string };
    const next = payload.access ?? "";
    if (next) await AsyncStorage.setItem(AUTH_ACCESS_KEY, next);
    return next;
  }, []);

  const apiGet = useCallback(
    async (path: string) => {
      const s = await loadSession();
      if (!s) return { ok: false, status: 0, body: "Not logged in" };
      const url = `${s.baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
      const doFetch = (access: string) => fetch(url, { method: "GET", headers: { Authorization: `Bearer ${access}` } });
      let resp = await doFetch(s.access);
      if (resp.status === 401) {
        const next = await refreshAccess(s.baseUrl, s.refresh);
        if (next) resp = await doFetch(next);
      }
      const raw = await resp.text();
      return { ok: resp.ok, status: resp.status, body: raw };
    },
    [loadSession, refreshAccess],
  );

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        setLoading(true);
        try {
          if (!deviceId) setDeviceId(await getDeviceId());

          if (!gpsLat.trim() || !gpsLng.trim()) {
            try {
              const { status } = await Location.requestForegroundPermissionsAsync();
              if (status !== "granted") {
                setLocationError("Location permission denied.");
              } else {
                setLocationError("");
                try {
                  const pos = await Location.getCurrentPositionAsync({});
                  if (!cancelled) {
                    setGpsLat(pos.coords.latitude.toFixed(6));
                    setGpsLng(pos.coords.longitude.toFixed(6));
                  }
                } catch {
                  const last = await Location.getLastKnownPositionAsync();
                  if (!cancelled && last?.coords) {
                    setGpsLat(last.coords.latitude.toFixed(6));
                    setGpsLng(last.coords.longitude.toFixed(6));
                  } else {
                    setLocationError("Current location is unavailable. Make sure that location services are enabled.");
                  }
                }
              }
            } catch {
              setLocationError("Current location is unavailable. Make sure that location services are enabled.");
            }
          }

          const ref = await apiGet("/api/v1/mobile/reference/");
          if (ref.ok) {
            try {
              const payload = JSON.parse(ref.body) as any;
              if (!cancelled) {
                const provinces = Array.isArray(payload?.provinces) ? payload.provinces : [];
                setProvinceOptions(provinces.map((p: any) => ({ label: String(p.label), value: String(p.code) })));

                const d = Array.isArray(payload?.districts) ? payload.districts : [];
                setDistricts(d.map((x: any) => ({ name: String(x.name), province: String(x.province), code: String(x.code ?? "") })));

                const cropStages = Array.isArray(payload?.crop_stages)
                  ? payload.crop_stages
                  : Array.isArray(payload?.cropStages)
                    ? payload.cropStages
                    : [];
                setCropStageOptions(cropStages.map((c: any) => ({ label: String(c.label), value: String(c.code) })));

                const tobaccoTypes = Array.isArray(payload?.tobacco_types)
                  ? payload.tobacco_types
                  : Array.isArray(payload?.tobaccoTypes)
                    ? payload.tobaccoTypes
                    : [];
                setTobaccoTypeOptions(tobaccoTypes.map((t: any) => ({ label: String(t.label), value: String(t.code) })));

                const barns = Array.isArray(payload?.barn_types)
                  ? payload.barn_types
                  : Array.isArray(payload?.barnTypes)
                    ? payload.barnTypes
                    : [];
                setBarnTypeOptions(barns.map((b: any) => ({ label: String(b.label), value: String(b.code) })));

                const st = Array.isArray(payload?.stakeholders) ? payload.stakeholders : [];
                setStakeholders(st.map((s: any) => ({ id: String(s.id), name: String(s.name), code: String(s.code ?? "") })));

                const sp = Array.isArray(payload?.sponsors) ? payload.sponsors : [];
                setSponsors(sp.map((s: any) => ({ id: String(s.id), name: String(s.name), code: String(s.code ?? "") })));
              }
            } catch {
            }
          }

          const fetchRefOptions = async (path: string): Promise<RefOption[]> => {
            const resp = await apiGet(path);
            if (!resp.ok) return [];
            try {
              const data = JSON.parse(resp.body) as any;
              const rows = Array.isArray(data) ? data : data?.results ?? [];
              return rows
                .filter((x: any) => x && (x.code || x.label))
                .map((x: any) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
            } catch {
              return [];
            }
          };

          const fetchAllPages = async <T,>(path: string, maxPages = 25): Promise<T[]> => {
            const out: T[] = [];
            const hasQuery = path.includes("?");
            for (let page = 1; page <= maxPages; page++) {
              const resp = await apiGet(`${path}${hasQuery ? "&" : "?"}page=${page}`);
              if (!resp.ok) return out;
              try {
                const payload = JSON.parse(resp.body) as any;
                if (Array.isArray(payload)) {
                  out.push(...(payload as T[]));
                  return out;
                }
                const rows = Array.isArray(payload?.results) ? (payload.results as T[]) : [];
                out.push(...rows);
                if (!payload?.next || rows.length === 0) return out;
              } catch {
                return out;
              }
            }
            return out;
          };

          if (cropStageOptions.length === 0) {
            const cached = await readCacheItems<any>("ref_crop_stages");
            if (!cancelled && cached.length > 0) {
              const rows = cached
                .filter((x) => x && (x.code || x.label))
                .map((x) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
              if (rows.length > 0) setCropStageOptions(rows);
            }
            const opts = await fetchRefOptions("/api/v1/mobile/reference/crop-stages/");
            if (!cancelled && opts.length > 0) {
              setCropStageOptions(opts);
              void writeCache("ref_crop_stages", opts);
            }
          }
          if (tobaccoTypeOptions.length === 0) {
            const cached = await readCacheItems<any>("ref_tobacco_types");
            if (!cancelled && cached.length > 0) {
              const rows = cached
                .filter((x) => x && (x.code || x.label))
                .map((x) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
              if (rows.length > 0) setTobaccoTypeOptions(rows);
            }
            const opts = await fetchRefOptions("/api/v1/mobile/reference/tobacco-types/");
            if (!cancelled && opts.length > 0) {
              setTobaccoTypeOptions(opts);
              void writeCache("ref_tobacco_types", opts);
            }
          }
          if (barnTypeOptions.length === 0) {
            const cached = await readCacheItems<any>("ref_barn_types");
            if (!cancelled && cached.length > 0) {
              const rows = cached
                .filter((x) => x && (x.code || x.label))
                .map((x) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
              if (rows.length > 0) setBarnTypeOptions(rows);
            }
            const opts = await fetchRefOptions("/api/v1/mobile/reference/barn-types/");
            if (!cancelled && opts.length > 0) {
              setBarnTypeOptions(opts);
              void writeCache("ref_barn_types", opts);
            }
          }

          if (sponsors.length === 0) {
            const cached = await readCacheItems<any>("sponsors");
            if (!cancelled && cached.length > 0) {
              setSponsors(cached.map((s) => ({ id: String(s.id), name: String(s.name), code: String((s as any).code ?? "") })));
            }
          }

          const sponsorRows = await fetchAllPages<{ id: string; name: string; code: string }>(
            "/api/v1/growers/sponsors/?ordering=name&page_size=250",
          );
          if (!cancelled && sponsorRows.length > 0) {
            setSponsors(
              sponsorRows.map((s) => ({ id: String(s.id), name: String(s.name), code: String((s as any).code ?? "") })),
            );
            void writeCache("sponsors", sponsorRows as any);
          }

          if (inspectionId) {
            const insp = await apiGet(`/api/v1/inspectorate/inspections/${encodeURIComponent(inspectionId)}/`);
            if (insp.ok) {
              try {
                const payload = JSON.parse(insp.body) as any;
                if (cancelled) return;
                setInspection(payload);

                const portalGrowerId = String(payload.grower ?? "");
                const growerChanged = Boolean(portalGrowerId) && portalGrowerId !== growerPortalId;
                if (growerChanged) {
                  setNrcNumber("");
                  setSex("");
                }
                if (portalGrowerId) setGrowerPortalId(portalGrowerId);
                setGrowerName(String(payload.grower_name ?? ""));
                setGrowerTbzId(String(payload.grower_tbz_id ?? ""));
                if (payload.province) setProvince(String(payload.province));
                if (payload.district) setDistrict(String(payload.district));

                if (portalGrowerId && (growerChanged || !nrcNumber || !sex)) {
                  const g = await apiGet(`/api/v1/growers/growers/${encodeURIComponent(portalGrowerId)}/`);
                  if (g.ok) {
                    try {
                      const gp = JSON.parse(g.body) as any;
                      if (!cancelled) {
                        if (gp.nrc_number && (growerChanged || !nrcNumber)) setNrcNumber(gp.nrc_number);
                        if (gp.sex && (growerChanged || !sex)) setSex(String(gp.sex));
                        if (gp.province && growerChanged) setProvince(String(gp.province));
                        if (gp.district && growerChanged) setDistrict(gp.district);
                      }
                    } catch {
                    }
                  }
                }
              } catch {
              }
            }
          }
        } finally {
          if (!cancelled) setLoading(false);
        }
      })();
      return () => {
        cancelled = true;
      };
    }, [apiGet, deviceId, growerPortalId, inspectionId, nrcNumber, sex]),
  );

  useEffect(() => {
    if (!province) {
      setDistrictOptions([]);
      return;
    }
    const opts = districts
      .filter((d) => d.province === province)
      .map((d) => ({ label: d.name, value: d.name }));
    setDistrictOptions(opts);
  }, [districts, province]);

  useEffect(() => {
    if (provinceOptions.length === 0) return;
    const next = normalizeFromOptions(province, provinceOptions);
    if (next && next !== province) setProvince(next);
  }, [province, provinceOptions]);

  useEffect(() => {
    const v = String(sex ?? "").trim();
    if (!v) return;
    const up = v.toUpperCase();
    const next = up === "MALE" || up === "M" ? "MALE" : up === "FEMALE" || up === "F" ? "FEMALE" : v;
    if (next !== sex) setSex(next);
  }, [sex]);

  useEffect(() => {
    if (cropStageOptions.length === 0) return;
    const next = normalizeFromOptions(cropStage, cropStageOptions);
    if (next && next !== cropStage) setCropStage(next);
  }, [cropStage, cropStageOptions]);

  useEffect(() => {
    if (tobaccoTypeOptions.length === 0) return;
    const next = normalizeFromOptions(tobaccoType, tobaccoTypeOptions);
    if (next && next !== tobaccoType) setTobaccoType(next);
  }, [tobaccoType, tobaccoTypeOptions]);

  useEffect(() => {
    if (barnTypeOptions.length === 0) return;
    const next = normalizeFromOptions(barnType, barnTypeOptions);
    if (next && next !== barnType) setBarnType(next);
  }, [barnType, barnTypeOptions]);

  useEffect(() => {
    const next = calculateYieldPerHa(validatedHectarage);
    if (!next) return;
    setYieldPerHa(next);
  }, [validatedHectarage]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!growerPortalId || tobaccoType) return;
      const resp = await apiGet(
        `/api/v1/growers/crop-allocations/?grower_season__grower=${encodeURIComponent(growerPortalId)}&ordering=-created_at&page_size=1`,
      );
      if (!resp.ok) return;
      try {
        const payload = JSON.parse(resp.body) as any;
        const rows = Array.isArray(payload) ? payload : payload?.results ?? [];
        const latest = rows[0];
        const tt = String(latest?.tobacco_type ?? "").trim();
        if (!cancelled && tt) setTobaccoType(tt);
      } catch {
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [apiGet, growerPortalId, tobaccoType]);

  const canSubmit = useMemo(() => {
    return Boolean(
      growerPortalId &&
        sex &&
        gpsLat.trim() &&
        gpsLng.trim() &&
        cropStage &&
        tobaccoType &&
        tobaccoVariety.trim() &&
        validatedHectarage.trim() &&
        yieldPerHa.trim() &&
        barnType &&
        barnCount.trim() &&
        capacityEnough &&
        province &&
        district,
    );
  }, [
    barnCount,
    barnType,
    capacityEnough,
    cropStage,
    district,
    gpsLat,
    gpsLng,
    growerPortalId,
    province,
    tobaccoType,
    tobaccoVariety,
    validatedHectarage,
    yieldPerHa,
    sex,
  ]);

  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (!growerPortalId) missing.push("Grower");
    if (!sex) missing.push("Sex");
    if (!gpsLat.trim()) missing.push("GPS Latitude");
    if (!gpsLng.trim()) missing.push("GPS Longitude");
    if (!cropStage) missing.push("Crop Stage");
    if (!tobaccoType) missing.push("Tobacco Type");
    if (!tobaccoVariety.trim()) missing.push("Tobacco Variety");
    if (!validatedHectarage.trim()) missing.push("Validated Hectarage");
    if (!yieldPerHa.trim()) missing.push("Yield per Ha (kg)");
    if (!barnType) missing.push("Types of Barns");
    if (!barnCount.trim()) missing.push("Number of Barns");
    if (!capacityEnough) missing.push("Is Barn Capacity Sufficient?");
    if (!province) missing.push("Province");
    if (!district) missing.push("District");
    return missing;
  }, [
    barnCount,
    barnType,
    capacityEnough,
    cropStage,
    district,
    gpsLat,
    gpsLng,
    growerPortalId,
    province,
    tobaccoType,
    tobaccoVariety,
    validatedHectarage,
    yieldPerHa,
    sex,
  ]);

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.headerRow}>
          {loading ? <LeafLoader size={22} /> : null}
        </View>
        <ThemedText style={styles.subtitle} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
          Verify grower identity, crop details, barn capacity, and stakeholders present. GPS coordinates are captured automatically.
        </ThemedText>

        {inspection ? (
          <View style={[styles.infoPanel, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={styles.infoRow}>
              <MaterialIcons name="calendar-month" size={18} color={Colors[theme].primary} />
              <ThemedText>
                <ThemedText type="defaultSemiBold">Inspection:</ThemedText> {inspection.inspection_type_display ?? "-"} scheduled{" "}
                {inspection.scheduled_date ?? "-"}
              </ThemedText>
            </View>
            <ThemedText>
              <ThemedText type="defaultSemiBold">Grower:</ThemedText> {(inspection.grower_name ?? growerName) || "-"}{" "}
              <ThemedText type="defaultSemiBold">{growerTbzId || "—"}</ThemedText>
              {nrcNumber ? ` | NRC: ${nrcNumber}` : ""}
            </ThemedText>
          </View>
        ) : null}

        <View style={styles.section}>
          <ThemedText type="subtitle">Grower Identity</ThemedText>
          <TextField label="Grower *" value={growerName} onChangeText={setGrowerName} editable={false} required />
          <View style={[styles.infoPanel, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={styles.infoRow}>
              <MaterialIcons name="badge" size={18} color={Colors[theme].primary} />
              <ThemedText>
                <ThemedText type="defaultSemiBold">TBZ ID:</ThemedText> {growerTbzId || "—"}
              </ThemedText>
            </View>
          </View>
          <TextField label="NRC Number" value={nrcNumber} onChangeText={setNrcNumber} editable={false} />
          <SelectField label="Sex *" value={sex} onChange={setSex} options={SEX_OPTIONS} required />
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">GPS</ThemedText>
          <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
            GPS coordinates are captured automatically.
          </ThemedText>
          <TextField label="GPS Latitude" value={gpsLat} onChangeText={setGpsLat} keyboardType="decimal-pad" required />
          <TextField label="GPS Longitude" value={gpsLng} onChangeText={setGpsLng} keyboardType="decimal-pad" required />
          {locationError ? (
            <ThemedText lightColor="#DC2626" darkColor="#DC2626">
              {locationError}
            </ThemedText>
          ) : null}
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Crop Details</ThemedText>
          <SelectField
            label="Crop Stage *"
            value={cropStage}
            onChange={setCropStage}
            options={cropStageOptions.length > 0 ? cropStageOptions : FALLBACK_CROP_STAGE_OPTIONS}
            required
          />
          <SelectField
            label="Tobacco Type *"
            value={tobaccoType}
            onChange={setTobaccoType}
            options={tobaccoTypeOptions.length > 0 ? tobaccoTypeOptions : FALLBACK_TOBACCO_TYPE_OPTIONS}
            required
          />
          <TextField label="Tobacco Variety *" value={tobaccoVariety} onChangeText={setTobaccoVariety} required />
          <TextField label="Validated Hectarage *" value={validatedHectarage} onChangeText={setValidatedHectarage} keyboardType="decimal-pad" required />
          <TextField label="Yield per Ha (kg) *" value={yieldPerHa} onChangeText={setYieldPerHa} keyboardType="decimal-pad" required />
          <SelectField
            label="Sponsor"
            value={sponsorId}
            onChange={setSponsorId}
            options={sponsorOptions}
            placeholder="Self-Sponsored"
          />
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Barn Capacity</ThemedText>
          <SelectField
            label="Types of Barns *"
            value={barnType}
            onChange={setBarnType}
            options={barnTypeOptions.length > 0 ? barnTypeOptions : FALLBACK_BARN_TYPE_OPTIONS}
            required
          />
          <TextField label="Number of Barns *" value={barnCount} onChangeText={setBarnCount} keyboardType="number-pad" required />
          <View style={styles.inline}>
            <ThemedText type="defaultSemiBold">Is Barn Capacity Sufficient? *</ThemedText>
            <View style={styles.pills}>
              <TogglePill label="YES" selected={capacityEnough === "Yes"} onPress={() => setCapacityEnough("Yes")} />
              <TogglePill label="NO" selected={capacityEnough === "No"} onPress={() => setCapacityEnough("No")} />
            </View>
          </View>
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Stakeholders Present</ThemedText>
          <View style={styles.pills}>
            {stakeholders.map((s) => {
              const selected = stakeholderIds.includes(s.id);
              return (
                <TogglePill
                  key={s.id}
                  label={s.name}
                  selected={selected}
                  onPress={() => {
                    setStakeholderIds((prev) => (selected ? prev.filter((x) => x !== s.id) : [...prev, s.id]));
                  }}
                />
              );
            })}
          </View>
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Location</ThemedText>
          <SelectField
            label="Province *"
            value={province}
            onChange={(v) => {
              setProvince(v);
              setDistrict("");
            }}
            options={provinceOptions}
            required
          />
          {districtOptions.length > 0 ? (
            <SelectField label="District *" value={district} onChange={setDistrict} options={districtOptions} disabled={!province} required />
          ) : (
            <TextField label="District *" value={district} onChangeText={setDistrict} required />
          )}
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Submission</ThemedText>
          <TextField label="Device ID" value={deviceId} onChangeText={setDeviceId} />
          <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
            Auto-populated by mobile app. Leave blank if using web.
          </ThemedText>
          <TextField label="Remark by Inspector" value={remark} onChangeText={setRemark} multiline />
          <View style={[styles.auditPanel, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <MaterialIcons name="shield" size={18} color={Colors[theme].primary} />
            <ThemedText>
              <ThemedText type="defaultSemiBold">Date/Time:</ThemedText> {new Date().toLocaleString()}
            </ThemedText>
          </View>
        </View>

        <PrimaryButton
          title="Submit Grower Validation"
          disabled={false}
          onPress={async () => {
            if (!canSubmit) {
              Alert.alert("Missing fields", `Complete all mandatory fields:\n\n${missingFields.join("\n")}`);
              return;
            }
            const payload: GrowerValidationPayload = {
              nrcNumber: nrcNumber.trim(),
              sex,
              gpsLatitude: gpsLat.trim(),
              gpsLongitude: gpsLng.trim(),
              cropStage,
              tobaccoType,
              tobaccoVariety: tobaccoVariety.trim(),
              validatedHectarage: validatedHectarage.trim(),
              yieldPerHa: yieldPerHa.trim(),
              sponsorId: sponsorId || "",
              barnType,
              barnCount: barnCount.trim(),
              barnCapacitySufficient: capacityEnough === "Yes",
              stakeholderIds,
              province,
              district,
              deviceId: deviceId.trim(),
              inspectorRemarks: remark.trim(),
            };

            const grower: GrowerProfile = {
              portalGrowerId: growerPortalId,
              growerId: growerTbzId || growerPortalId,
              nrc: nrcNumber.trim(),
              name: growerName,
              sponsor: sponsorName,
              province: provinceLabel,
              district,
              hectarage: 0,
            };

            const { generateId } = await import("@/lib/inspection-storage");
            const report: InspectionReport = {
              id: params.editingReportId?.toString() || generateId(),
              stage: "Grower Validation",
              grower: {
                name: growerName.trim(),
                growerId: growerTbzId.trim(),
                portalGrowerId: growerPortalId,
                nrc: nrcNumber.trim(),
                province: provinceLabel.trim(),
                district: district.trim(),
                sponsor: sponsorName,
                hectarage: initialGrower?.hectarage || 0,
              },
              audit: {
                inspectorName: "Auto",
                submittedAt: new Date().toISOString(),
                gps: `${gpsLat.trim()}, ${gpsLng.trim()}`,
                deviceId: deviceId.trim(),
              },
              payload,
              inspectionId,
              syncStatus: "pending",
              lastError: "",
            };

            await saveInspectionReport(report);
            Alert.alert("Saved", "Saved to Conducted (Pending Sync).", [
              { text: "View Pending Sync", onPress: () => router.replace("/inspection/reports" as any) },
              { text: "Back", onPress: () => router.replace("/inspection" as any) },
            ]);
          }}
        />
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: 20,
    gap: 14,
  },
  headerRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  subtitle: { lineHeight: 18 },
  small: { fontSize: 12, lineHeight: 16 },
  infoPanel: { borderWidth: 1, borderRadius: 16, padding: 12, gap: 8 },
  infoRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  section: {
    gap: 10,
  },
  inline: {
    gap: 8,
  },
  pills: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10,
  },
  pill: {
    borderWidth: 1,
    borderRadius: 999,
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  auditPanel: {
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },
});
