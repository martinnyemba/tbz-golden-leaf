import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { StyleSheet, ScrollView, View, Alert, Pressable, KeyboardAvoidingView, Platform, Modal } from 'react-native';
import { useRouter } from 'expo-router';
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect } from 'expo-router';
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { LeafLoader } from '@/components/LeafLoader';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { TextField, SelectField } from '@/components/ui/form-controls';
import {
  apiErrorUi,
  apiFetchJson,
  extractErrorMessage,
  savePendingSaleCapture,
  getPendingSaleCaptures,
  lookupPermitOffline,
  type BalePayload,
} from '@/lib/inspection-storage';
import { useNetworkStatus } from '@/lib/network-state';
import { Colors } from '@/constants/theme';
import { FontFamily, FontWeight } from '@/constants/typography';

const TOBACCO_TYPES = [
  { label: 'Flue Cured Tobacco', value: 'FLUE_CURED' },
  { label: 'Burley', value: 'BURLEY' },
  { label: 'Dark Fired Tobacco', value: 'DARK_FIRED' },
];

const BALE_OUTCOME_OPTIONS = [
  { label: "Bought", value: "BOUGHT" },
  { label: "Rejected", value: "REJECTED" },
];

const BALE_REJECTION_REASON_OPTIONS = [
  { label: "Nested", value: "NESTED" },
  { label: "High Moisture", value: "HIGH_MOISTURE" },
  { label: "Low Moisture", value: "LOW_MOISTURE" },
  { label: "NTRM", value: "NTRM" },
  { label: "Overweight", value: "OVERWEIGHT" },
  { label: "Underweight", value: "UNDERWEIGHT" },
  { label: "No Sale", value: "NO_SALE" },
];

const VALIDATED_PERMIT_KEY = "tbz:marketingCaptureValidatedPermit:v1";

function ActionButton({
  title,
  onPress,
  disabled,
  loading,
}: {
  title: string;
  onPress: () => void | Promise<void>;
  disabled?: boolean;
  loading?: boolean;
}) {
  const busy = Boolean(loading);
  const isDisabled = Boolean(disabled) || busy;
  return (
    <Pressable
      onPress={() => {
        if (isDisabled) return;
        const res = onPress();
        if (res && typeof (res as any).then === "function") {
          (res as Promise<unknown>).catch((e) => {
            const msg = e instanceof Error ? e.message : String(e ?? "Error");
            Alert.alert("Error", extractErrorMessage(msg) || "An unexpected error occurred.");
          });
        }
      }}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.actionBtn,
        isDisabled && styles.actionBtnDisabled,
        pressed && !isDisabled && styles.actionBtnPressed,
      ]}
    >
      {busy ? <LeafLoader size={18} color="#FFFFFF" /> : null}
      <ThemedText style={styles.actionBtnText} numberOfLines={1}>
        {title}
      </ThemedText>
    </Pressable>
  );
}

function SecondaryButton({
  title,
  icon,
  onPress,
  disabled,
}: {
  title: string;
  icon?: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  const isDisabled = Boolean(disabled);
  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.secondaryBtn,
        isDisabled && styles.secondaryBtnDisabled,
        pressed && !isDisabled && styles.secondaryBtnPressed,
      ]}
    >
      {icon ? <MaterialIcons name={icon as any} size={20} color="#0B6B3A" /> : null}
      <ThemedText style={styles.secondaryBtnText} numberOfLines={1}>
        {title}
      </ThemedText>
    </Pressable>
  );
}

export default function SalesCaptureScreen() {
  const router = useRouter();
  const theme = "light";
  const insets = useSafeAreaInsets();
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [step, setStep] = useState(1);
  const [permission, requestPermission] = useCameraPermissions();

  const [permitToken, setPermitToken] = useState("");
  const [salesfloorId, setSalesfloorId] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");
  const [buyerId, setBuyerId] = useState("");
  const [tobaccoType, setTobaccoType] = useState("");
  const [season, setSeason] = useState(new Date().getFullYear().toString() + "/" + (new Date().getFullYear() + 1).toString());
  const [saleDate, setSaleDate] = useState(new Date().toISOString().split("T")[0]);
  const [growerRepName, setGrowerRepName] = useState("");
  const [permitStatusText, setPermitStatusText] = useState("");
  const [permitFromCache, setPermitFromCache] = useState(false);

  const [salesfloors, setSalesfloors] = useState<any[]>([]);
  const [buyers, setBuyers] = useState<any[]>([]);
  const [loadingSalesfloors, setLoadingSalesfloors] = useState(false);
  const [salesfloorFetchStatus, setSalesfloorFetchStatus] = useState<number | null>(null);
  const [salesfloorFetchError, setSalesfloorFetchError] = useState<string>("");

  const [validatedGrower, setValidatedGrower] = useState<any>(null);
  const [validatedPermit, setValidatedPermit] = useState<any>(null);
  const [isValidating, setIsValidating] = useState(false);

  const [bales, setBales] = useState<any[]>([
    { id: Date.now(), ticket: "", grade: "", weight: "", moisture: "", price: "", outcome: "BOUGHT", rejectionReason: "" },
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [recentBales, setRecentBales] = useState<any[]>([]);
  const [pendingSalesCount, setPendingSalesCount] = useState(0);

  const [isScannerOpen, setIsScannerOpen] = useState(false);
  const [scanContext, setScanContext] = useState<{ type: 'permit' | 'bale', rowId: number | null }>({ type: 'permit', rowId: null });
  const [scanned, setScanned] = useState(false);

  const safeJson = useCallback((raw: string) => {
    try {
      return { ok: true as const, value: JSON.parse(raw) };
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Invalid JSON";
      return { ok: false as const, error: msg };
    }
  }, []);

  const salesfloorLabel = useCallback((sf: any) => {
    return String(sf?.name ?? sf?.salesfloor_name ?? sf?.legal_name ?? sf?.company_name ?? "").trim();
  }, []);

  const buyerLabel = useCallback((b: any) => {
    return String(b?.company_name ?? b?.name ?? b?.legal_name ?? "").trim();
  }, []);

  async function fetchAllEntities(entityType: string) {
    const collected: any[] = [];
    let nextPath: string | null = `/api/v1/entities/legal-entities/?entity_type=${encodeURIComponent(entityType)}&page_size=250`;

    while (nextPath) {
      const resp = await apiFetchJson(nextPath, { method: "GET" });
      setSalesfloorFetchStatus(resp.status);

      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        throw new Error(ui.message);
      }

      const parsed = safeJson(resp.body);
      if (!parsed.ok) throw new Error(`Parse error: ${parsed.error}`);

      const payload = parsed.value;
      const rows = Array.isArray(payload) ? payload : (payload.results ?? []);
      collected.push(...rows);

      const nextRaw = Array.isArray(payload) ? null : (payload.next ?? null);
      if (!nextRaw || typeof nextRaw !== "string") {
        nextPath = null;
        continue;
      }

      try {
        const nextUrl = new URL(nextRaw);
        nextPath = `${nextUrl.pathname}${nextUrl.search}`;
      } catch {
        nextPath = nextRaw.startsWith("/") ? nextRaw : `/${nextRaw}`;
      }
    }

    return Array.from(new Map(collected.map((x) => [String(x.id), x])).values());
  }

  const refreshPendingCount = useCallback(async () => {
    const all = await getPendingSaleCaptures();
    setPendingSalesCount(all.filter((s) => s.syncStatus !== "synced").length);
  }, []);

  useFocusEffect(
    useCallback(() => {
      async function fetchLookups() {
        setLoadingSalesfloors(true);
        setSalesfloorFetchError("");
        setSalesfloorFetchStatus(null);
        try {
          const [sfs, bys] = await Promise.all([
            fetchAllEntities("SALES_FLOOR"),
            fetchAllEntities("BUYER"),
          ]);
          setSalesfloors(sfs.filter((sf) => sf?.is_active !== false));
          setBuyers(bys.filter((b) => b?.is_active !== false).map((b) => ({ ...b, company_name: b.company_name ?? b.name })));
        } catch (e) {
          const msg = e instanceof Error ? e.message : "Failed to load sales floors";
          setSalesfloorFetchError(msg);
          if (!isOffline) Alert.alert("Failed to load sales floors", msg);
          setSalesfloors([]);
          setBuyers([]);
        } finally {
          setLoadingSalesfloors(false);
        }
      }

      void fetchLookups();
      void refreshPendingCount();
    }, [isOffline, refreshPendingCount])
  );

  const sfOptions = useMemo(
    () =>
      salesfloors
        .map((sf) => ({ label: salesfloorLabel(sf), value: String(sf?.id ?? "") }))
        .filter((o) => Boolean(o.label) && Boolean(o.value)),
    [salesfloorLabel, salesfloors],
  );
  const buyerOptions = useMemo(
    () =>
      buyers
        .map((b) => ({ label: buyerLabel(b), value: String(b?.id ?? "") }))
        .filter((o) => Boolean(o.label) && Boolean(o.value)),
    [buyerLabel, buyers],
  );

  const isPermitValidated = useMemo(() => Boolean(validatedGrower && validatedPermit), [validatedGrower, validatedPermit]);

  const restoreValidatedPermit = useCallback(async () => {
    try {
      const raw = await AsyncStorage.getItem(VALIDATED_PERMIT_KEY);
      if (!raw) return;
      const parsed = safeJson(raw);
      if (!parsed.ok) return;
      const payload = parsed.value as any;
      if (!payload?.permit || !payload?.grower) return;
      setValidatedPermit(payload.permit);
      setValidatedGrower(payload.grower);
      setPermitToken(String(payload.permit_token ?? ""));
      const savedSalesfloorId = String(payload.salesfloor_id ?? payload.salesfloor?.id ?? "").trim();
      if (savedSalesfloorId) {
        setSalesfloorId(savedSalesfloorId);
      } else {
        const savedName = String(payload.salesfloor_name ?? payload.salesfloor?.name ?? "").trim();
        const match = salesfloors.find((sf) => salesfloorLabel(sf) === savedName);
        setSalesfloorId(String(match?.id ?? ""));
      }
      setProvince(String(payload.salesfloor?.province ?? payload.grower?.province ?? ""));
      setDistrict(String(payload.salesfloor?.district ?? payload.grower?.district ?? ""));
      setPermitStatusText("Permit previously validated — ready to capture.");
      setPermitFromCache(Boolean(payload.fromCache));
      setStep(2);
    } catch {}
  }, [safeJson, salesfloorLabel, salesfloors]);

  useEffect(() => {
    void restoreValidatedPermit();
  }, [restoreValidatedPermit]);

  const clearValidatedPermit = useCallback(async () => {
    await AsyncStorage.removeItem(VALIDATED_PERMIT_KEY);
    setPermitToken("");
    setSalesfloorId("");
    setProvince("");
    setDistrict("");
    setBuyerId("");
    setTobaccoType("");
    setSeason(new Date().getFullYear().toString() + "/" + (new Date().getFullYear() + 1).toString());
    setSaleDate(new Date().toISOString().split("T")[0]);
    setGrowerRepName("");
    setValidatedGrower(null);
    setValidatedPermit(null);
    setPermitStatusText("");
    setPermitFromCache(false);
    setBales([{ id: Date.now(), ticket: "", grade: "", weight: "", moisture: "", price: "", outcome: "BOUGHT", rejectionReason: "" }]);
    setStep(1);
  }, []);

  const fetchRecentBales = async () => {
    if (isOffline) return;
    try {
      const today = new Date().toISOString().split("T")[0];
      const resp = await apiFetchJson(`/api/v1/marketing/bales/?sale_date=${encodeURIComponent(today)}&ordering=-created_at&page_size=15`, { method: "GET" });
      if (!resp.ok) return;
      const parsed = safeJson(resp.body);
      if (!parsed.ok) return;
      const data = parsed.value;
      setRecentBales(Array.isArray(data) ? data : (data.results ?? []));
    } catch {}
  };

  useEffect(() => {
    void fetchRecentBales();
  }, []);

  const openScanner = async (type: 'permit' | 'bale', rowId: number | null = null) => {
    if (!permission?.granted) {
      const { granted } = await requestPermission();
      if (!granted) {
        Alert.alert("Camera Permission Required", "Please allow camera access to scan barcodes.");
        return;
      }
    }
    setScanContext({ type, rowId });
    setScanned(false);
    setIsScannerOpen(true);
  };

  const handleBarCodeScanned = ({ data }: BarcodeScanningResult) => {
    setScanned(true);
    setIsScannerOpen(false);
    if (scanContext.type === 'permit') {
      setPermitToken(data);
    } else if (scanContext.type === 'bale' && scanContext.rowId) {
      updateBale(scanContext.rowId, 'ticket', data);
    }
  };

  const handleVerify = async () => {
    if (!permitToken.trim() || !salesfloorId) {
      Alert.alert("Missing Input", "Permit Token and Sales floor are required.");
      return;
    }

    setIsValidating(true);
    try {
      const sfMatch = salesfloors.find((sf) => String(sf?.id ?? "") === String(salesfloorId));
      if (!sfMatch) throw new Error("Invalid salesfloor selected.");

      if (isOffline) {
        // Offline: try to find the permit in local cache by scanning the token text as a permit number
        const { permit, grower } = await lookupPermitOffline(permitToken.trim());
        if (!permit || !grower) {
          Alert.alert(
            "Offline – Permit Not Found",
            "No cached permit matches this token. Connect to the internet to validate new permits.",
          );
          return;
        }
        setValidatedPermit(permit);
        setValidatedGrower(grower);
        setProvince(sfMatch.province || grower.province || "");
        setDistrict(sfMatch.district || grower.district || "");
        setPermitStatusText("Permit loaded from offline cache. Data may be stale.");
        setPermitFromCache(true);
        await AsyncStorage.setItem(
          VALIDATED_PERMIT_KEY,
          JSON.stringify({
            permit_token: permitToken.trim(),
            salesfloor_id: String(sfMatch.id),
            salesfloor_name: salesfloorLabel(sfMatch),
            permit,
            grower,
            salesfloor: { id: String(sfMatch.id), name: salesfloorLabel(sfMatch), province: sfMatch.province, district: sfMatch.district },
            validated_at: new Date().toISOString(),
            fromCache: true,
          }),
        );
        setStep(2);
        return;
      }

      const verifyResp = await apiFetchJson("/api/v1/permits/verify-qr/", {
        method: "POST",
        body: JSON.stringify({ permit_token: permitToken.trim(), salesfloor_id: String(sfMatch.id) }),
      });

      const parsedVerify = safeJson(verifyResp.body);
      if (!parsedVerify.ok || !verifyResp.ok) {
        const ui = apiErrorUi(verifyResp.status, verifyResp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/sales" } } as any) },
            { text: "OK" },
          ]);
        } else if (ui.kind === "network_error") {
          // Fallback to cache on network error
          const { permit, grower } = await lookupPermitOffline(permitToken.trim());
          if (permit && grower) {
            setValidatedPermit(permit);
            setValidatedGrower(grower);
            setProvince(sfMatch.province || grower.province || "");
            setDistrict(sfMatch.district || grower.district || "");
            setPermitStatusText("Loaded from cache (network unavailable). Data may be stale.");
            setPermitFromCache(true);
            await AsyncStorage.setItem(
              VALIDATED_PERMIT_KEY,
              JSON.stringify({
                permit_token: permitToken.trim(),
                salesfloor_id: String(sfMatch.id),
                salesfloor_name: salesfloorLabel(sfMatch),
                permit,
                grower,
                salesfloor: { id: String(sfMatch.id), name: salesfloorLabel(sfMatch), province: sfMatch.province, district: sfMatch.district },
                validated_at: new Date().toISOString(),
                fromCache: true,
              }),
            );
            setStep(2);
          } else {
            Alert.alert(ui.title, ui.message);
          }
        } else {
          Alert.alert(ui.title, ui.message);
        }
        setValidatedGrower(null);
        setValidatedPermit(null);
        return;
      }

      const verifyBody = parsedVerify.value as any;
      if (verifyBody?.valid === false) {
        Alert.alert("Validation Error", String(verifyBody?.detail ?? verifyBody?.message ?? "Permit validation failed."));
        setValidatedGrower(null);
        setValidatedPermit(null);
        return;
      }

      const permitNumber = String(verifyBody?.permit_number ?? "").trim();
      const growerTbzId = String(verifyBody?.grower_tbz_id ?? "").trim();

      let p: any = null;
      if (permitNumber) {
        const permitResp = await apiFetchJson(
          `/api/v1/permits/transport-permits/?search=${encodeURIComponent(permitNumber)}&page_size=5`,
          { method: "GET" },
        );
        const parsedPermit = safeJson(permitResp.body);
        if (permitResp.ok && parsedPermit.ok) {
          const rows = Array.isArray(parsedPermit.value) ? parsedPermit.value : parsedPermit.value?.results ?? [];
          const match = rows.find((x: any) => String(x?.permit_number ?? "").trim() === permitNumber) ?? rows[0] ?? null;
          if (match) {
            const usedBales = Number(match?.used_bales ?? NaN);
            const totalBales = Number(match?.total_bales ?? NaN);
            const usedWeight = Number(match?.used_weight_kg ?? NaN);
            const totalWeight = Number(match?.total_weight_kg ?? NaN);
            p = {
              ...match,
              permit_number: match.permit_number ?? permitNumber,
              remaining_bales: match?.remaining_bales ?? (Number.isFinite(totalBales) && Number.isFinite(usedBales) ? Math.max(totalBales - usedBales, 0) : "—"),
              remaining_weight_kg: match?.remaining_weight_kg ?? (Number.isFinite(totalWeight) && Number.isFinite(usedWeight) ? Math.max(totalWeight - usedWeight, 0) : "—"),
            };
          }
        }
      }

      if (!p) p = { permit_number: permitNumber || "—", remaining_bales: "—", remaining_weight_kg: "—" };

      let g: any = null;
      if (p?.grower) {
        const growerResp = await apiFetchJson(`/api/v1/growers/growers/${encodeURIComponent(String(p.grower))}/`, { method: "GET" });
        const parsedGrower = safeJson(growerResp.body);
        if (growerResp.ok && parsedGrower.ok) {
          const gg = parsedGrower.value as any;
          g = { id: String(gg.id), display_name: gg.display_name, tbz_id: gg.tbz_id, province: gg.province, district: gg.district };
        }
      }

      if (!g && growerTbzId) {
        const resp = await apiFetchJson(`/api/v1/growers/growers/?search=${encodeURIComponent(growerTbzId)}&page_size=5`, { method: "GET" });
        const parsed = safeJson(resp.body);
        const payload = parsed.ok ? (parsed.value as any) : null;
        const rows = Array.isArray(payload) ? payload : payload?.results ?? [];
        const match = rows.find((x: any) => String(x?.tbz_id ?? "").trim() === growerTbzId) ?? rows[0] ?? null;
        if (match) {
          g = { id: String(match.id), display_name: match.display_name, tbz_id: match.tbz_id, province: match.province, district: match.district };
        }
      }

      if (!g || !String(g?.id ?? "").trim()) {
        Alert.alert("Validation Error", "Permit validated but grower could not be resolved.");
        setValidatedGrower(null);
        setValidatedPermit(null);
        return;
      }

      setValidatedGrower(g);
      setValidatedPermit(p);
      setProvince(sfMatch.province || g?.province || "");
      setDistrict(sfMatch.district || g?.district || "");
      setPermitStatusText("Permit validated. Marketing capture unlocked.");
      setPermitFromCache(false);
      await AsyncStorage.setItem(
        VALIDATED_PERMIT_KEY,
        JSON.stringify({
          permit_token: permitToken.trim(),
          salesfloor_id: String(sfMatch.id),
          salesfloor_name: salesfloorLabel(sfMatch),
          permit: p,
          grower: g,
          salesfloor: { id: String(sfMatch.id), name: salesfloorLabel(sfMatch), province: sfMatch.province, district: sfMatch.district },
          validated_at: new Date().toISOString(),
          fromCache: false,
        }),
      );
      setStep(2);
    } catch (e: any) {
      const raw = e instanceof Error ? e.message : String(e ?? "");
      Alert.alert("Error", extractErrorMessage(raw) || "An unexpected error occurred.");
      setValidatedGrower(null);
      setValidatedPermit(null);
    } finally {
      setIsValidating(false);
    }
  };

  const addBale = () => {
    if (bales.length >= 500) { Alert.alert("Limit Reached", "Max 500 bales per batch."); return; }
    setBales([...bales, { id: Date.now(), ticket: "", grade: "", weight: "", moisture: "", price: "", outcome: "BOUGHT", rejectionReason: "" }]);
  };

  const updateBale = (id: number, field: string, value: string) => {
    setBales(bales.map(b => b.id === id ? { ...b, [field]: value } : b));
  };

  const removeBale = (id: number) => {
    if (bales.length === 1) return;
    setBales(bales.filter(b => b.id !== id));
  };

  const moveNext = () => {
    if (step === 1 && (!validatedGrower || !validatedPermit)) {
      Alert.alert("Action Blocked", "Please validate the permit QR token first.");
      return;
    }
    if (step === 2 && (!province || !district || !tobaccoType || !season || !saleDate)) {
      Alert.alert("Missing Fields", "Complete all mandatory header fields before continuing.");
      return;
    }
    setStep(step + 1);
  };

  const buildSubmitPayload = useCallback((): { payload: { permit_token: string; grower: string; salesfloor: string; season: string; bales: BalePayload[] } | null; baleCount: number } => {
    const usable = bales.filter((b) => String(b.ticket ?? "").trim().length > 0);
    const sfMatch = salesfloors.find((sf) => String(sf?.id ?? "") === String(salesfloorId));
    const buyerMatch = buyers.find((b) => String(b?.id ?? "") === String(buyerId));
    const typeAlias = TOBACCO_TYPES.find(t => t.label === tobaccoType)?.value;
    if (!sfMatch || !validatedGrower?.id || !typeAlias) return { payload: null, baleCount: 0 };

    const balePayloads: BalePayload[] = usable.map(b => ({
      grower: validatedGrower.id,
      salesfloor: sfMatch.id,
      season: season.trim(),
      tobacco_type: typeAlias,
      buyer: buyerMatch?.id || undefined,
      sale_date: saleDate.trim(),
      grower_rep_name: growerRepName.trim() || undefined,
      bale_ticket_number: b.ticket.trim(),
      grade_mark: b.grade.trim(),
      weight_kg: parseFloat(b.weight) || 0,
      moisture_percent: b.moisture.trim() ? parseFloat(b.moisture) : undefined,
      price_per_kg: b.price.trim() ? parseFloat(b.price) : undefined,
      status: b.outcome === "REJECTED" ? "REJECTED" : "BOUGHT",
      rejection_reason: b.outcome === "REJECTED" ? (b.rejectionReason || undefined) : undefined,
    }));

    return {
      payload: {
        permit_token: permitToken.trim(),
        grower: validatedGrower.id,
        salesfloor: sfMatch.id,
        season: season.trim(),
        bales: balePayloads,
      },
      baleCount: usable.length,
    };
  }, [bales, salesfloors, salesfloorId, buyers, buyerId, tobaccoType, validatedGrower, season, saleDate, growerRepName, permitToken]);

  const validateBaleInputs = useCallback((): string | null => {
    const usable = bales.filter((b) => String(b.ticket ?? "").trim().length > 0);
    if (usable.length === 0) return "Enter at least one bale ticket number.";
    if (usable.length > 500) return "Bale count must be between 1 and 500.";
    const sfMatch = salesfloors.find((sf) => String(sf?.id ?? "") === String(salesfloorId));
    if (!sfMatch) return "Select a valid sales floor.";
    if (!TOBACCO_TYPES.find(t => t.label === tobaccoType)?.value) return "Tobacco type is required.";
    if (!validatedGrower?.id) return "Validate permit first.";
    if (!province.trim() || !district.trim()) return "Province and district are required.";
    if (!season.trim() || !saleDate.trim()) return "Season and sale date are required.";
    const invalidRow = usable.find((b) => {
      const gradeOk = String(b.grade ?? "").trim().length > 0;
      const weight = Number(String(b.weight ?? "").trim());
      const weightOk = Number.isFinite(weight) && weight >= 1;
      const outcome = String(b.outcome ?? "");
      const rejectionOk = outcome !== "REJECTED" || String(b.rejectionReason ?? "").trim().length > 0;
      return !gradeOk || !weightOk || !rejectionOk;
    });
    if (invalidRow) return "All rows need Grade, Weight (≥1 kg), and Rejection Reason when Rejected.";
    return null;
  }, [bales, salesfloors, salesfloorId, tobaccoType, validatedGrower, province, district, season, saleDate]);

  const resetAfterSuccess = useCallback(async () => {
    try {
      await clearValidatedPermit();
      setBuyerId("");
      setTobaccoType("");
      setSeason(new Date().getFullYear().toString() + "/" + (new Date().getFullYear() + 1).toString());
      setSaleDate(new Date().toISOString().split("T")[0]);
      setBales([{ id: Date.now(), ticket: "", grade: "", weight: "", moisture: "", price: "", outcome: "BOUGHT", rejectionReason: "" }]);
      setStep(1);
    } catch {}
  }, [clearValidatedPermit]);

  const handleSubmit = async () => {
    const validationError = validateBaleInputs();
    if (validationError) { Alert.alert("Missing Metrics", validationError); return; }

    const { payload, baleCount } = buildSubmitPayload();
    if (!payload) { Alert.alert("Missing fields", "Cannot build submission payload."); return; }

    const sfMatch = salesfloors.find((sf) => String(sf?.id ?? "") === String(salesfloorId));

    setIsSubmitting(true);
    try {
      if (isOffline) {
        await savePendingSaleCapture(payload, {
          baleCount,
          growerName: validatedGrower?.display_name,
          salesfloorName: sfMatch ? salesfloorLabel(sfMatch) : undefined,
          permitNumber: validatedPermit?.permit_number,
          saleDate: saleDate.trim(),
        });
        await refreshPendingCount();
        await resetAfterSuccess();
        Alert.alert(
          "Saved Offline",
          `${baleCount} bale(s) saved locally. They will be submitted when you reconnect.`,
          [{ text: "OK", onPress: () => router.back() }],
        );
        return;
      }

      const resp = await apiFetchJson("/api/v1/marketing/bales/bulk-create/", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      if (resp.ok) {
        await resetAfterSuccess();
        Alert.alert("Capture Successful", `${baleCount} bale(s) captured successfully.`, [
          { text: "Done", onPress: () => { router.back(); fetchRecentBales(); } },
        ]);
      } else {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "network_error") {
          // Save to offline queue on network failure
          await savePendingSaleCapture(payload, {
            baleCount,
            growerName: validatedGrower?.display_name,
            salesfloorName: sfMatch ? salesfloorLabel(sfMatch) : undefined,
            permitNumber: validatedPermit?.permit_number,
            saleDate: saleDate.trim(),
          });
          await refreshPendingCount();
          await resetAfterSuccess();
          Alert.alert(
            "Saved Offline",
            `Network unavailable. ${baleCount} bale(s) saved and will sync automatically when you reconnect.`,
            [{ text: "OK", onPress: () => router.back() }],
          );
        } else if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/sales" } } as any) },
            { text: "Save Offline", onPress: async () => {
              await savePendingSaleCapture(payload, { baleCount, growerName: validatedGrower?.display_name, salesfloorName: sfMatch ? salesfloorLabel(sfMatch) : undefined, permitNumber: validatedPermit?.permit_number, saleDate: saleDate.trim() });
              await refreshPendingCount();
              await resetAfterSuccess();
            }},
          ]);
        } else {
          Alert.alert(ui.title, ui.message, [
            { text: "OK" },
            { text: "Save Offline", onPress: async () => {
              await savePendingSaleCapture(payload, { baleCount, growerName: validatedGrower?.display_name, salesfloorName: sfMatch ? salesfloorLabel(sfMatch) : undefined, permitNumber: validatedPermit?.permit_number, saleDate: saleDate.trim() });
              await refreshPendingCount();
              await resetAfterSuccess();
              Alert.alert("Saved Offline", `${baleCount} bale(s) queued for later sync.`);
            }},
          ]);
        }
      }
    } catch {
      // Network exception — save to offline queue
      await savePendingSaleCapture(payload, {
        baleCount,
        growerName: validatedGrower?.display_name,
        salesfloorName: sfMatch ? salesfloorLabel(sfMatch) : undefined,
        permitNumber: validatedPermit?.permit_number,
        saleDate: saleDate.trim(),
      });
      await refreshPendingCount();
      await resetAfterSuccess();
      Alert.alert(
        "Saved Offline",
        `Network error. ${baleCount} bale(s) saved and will sync automatically when you reconnect.`,
        [{ text: "OK", onPress: () => router.back() }],
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      {/* SCANNER OVERLAY MODAL */}
      <Modal visible={isScannerOpen} animationType="slide" onRequestClose={() => setIsScannerOpen(false)}>
        <View style={styles.scannerWrapper}>
          <View style={styles.scannerHeader}>
            <ThemedText style={{ color: "#FFFFFF", fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>
              {scanContext.type === 'permit' ? "Scan Permit Token" : "Scan Bale Ticket"}
            </ThemedText>
            <Pressable onPress={() => setIsScannerOpen(false)}>
              <MaterialIcons name="close" size={28} color="#FFFFFF" />
            </Pressable>
          </View>
          <CameraView
            style={StyleSheet.absoluteFillObject}
            facing="back"
            barcodeScannerSettings={{
              barcodeTypes: scanContext.type === 'permit'
                ? ["qr"]
                : ["ean13", "ean8", "qr", "pdf417", "upc_e", "datamatrix", "code39", "code93", "itf14", "codabar", "code128", "upc_a"],
            }}
            onBarcodeScanned={scanned ? undefined : handleBarCodeScanned}
          />
          <View style={styles.scannerOverlay}>
            <View style={styles.scannerCutout} />
            <ThemedText style={{ color: '#FFFFFF', marginTop: 32, fontSize: 13, textAlign: 'center' }}>
              Align the barcode within the frame to scan.
            </ThemedText>
          </View>
        </View>
      </Modal>

      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : undefined}>
        {/* HEADER */}
        <View style={[styles.headerRow, { paddingTop: Math.max(12, insets.top + 10) }]}>
          <Pressable onPress={() => router.back()} style={{ padding: 8, marginLeft: -8, marginRight: 8 }}>
            <MaterialIcons name="arrow-back" size={26} color="#FFFFFF" />
          </Pressable>
          <View style={{ flex: 1 }}>
            <ThemedText type="subtitle" style={{ fontSize: 16, color: "#FFFFFF" }}>Sales Floor Bale Capture</ThemedText>
            <ThemedText style={{ fontSize: 12, color: "#D1FAE5" }}>Permit Validation → Create Batch → Capture Bales</ThemedText>
          </View>
          {pendingSalesCount > 0 && (
            <Pressable
              style={styles.pendingBadge}
              onPress={() => router.push("/pending-sales" as any)}
            >
              <MaterialIcons name="cloud-upload" size={14} color="#FFFFFF" />
              <ThemedText style={styles.pendingBadgeText}>{pendingSalesCount} pending</ThemedText>
            </Pressable>
          )}
        </View>

        {/* OFFLINE BANNER */}
        {isOffline && (
          <View style={styles.offlineBanner}>
            <MaterialIcons name="wifi-off" size={15} color="#FFFFFF" />
            <ThemedText style={styles.offlineBannerText}>
              You are offline — captures will be saved and synced when reconnected
            </ThemedText>
          </View>
        )}

        {/* CACHE WARNING */}
        {permitFromCache && isPermitValidated && (
          <View style={styles.cacheWarning}>
            <MaterialIcons name="warning" size={15} color="#92400E" />
            <ThemedText style={styles.cacheWarningText}>Permit data is from local cache and may be stale</ThemedText>
          </View>
        )}

        {/* STEPPER */}
        <View style={styles.stepperContainer}>
          <View style={[styles.stepDot, step >= 1 && styles.stepDotActive]}>
            <ThemedText style={[styles.stepDotText, step >= 1 && styles.stepDotTextActive]}>1</ThemedText>
          </View>
          <View style={[styles.stepLine, step >= 2 && styles.stepLineActive]} />
          <View style={[styles.stepDot, step >= 2 && styles.stepDotActive]}>
            <ThemedText style={[styles.stepDotText, step >= 2 && styles.stepDotTextActive]}>2</ThemedText>
          </View>
          <View style={[styles.stepLine, step >= 3 && styles.stepLineActive]} />
          <View style={[styles.stepDot, step >= 3 && styles.stepDotActive]}>
            <ThemedText style={[styles.stepDotText, step >= 3 && styles.stepDotTextActive]}>3</ThemedText>
          </View>
        </View>

        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {/* STEP 1: PERMIT VALIDATION */}
          {step === 1 && (
            <View style={styles.wizardCard}>
              <View style={styles.wizardCardTitleBar}>
                <MaterialIcons name="qr-code-scanner" size={20} color="#0B6B3A" style={{ marginRight: 8 }} />
                <ThemedText type="defaultSemiBold">Step 1 - Validate Permit</ThemedText>
              </View>
              <ThemedText style={{ fontSize: 12, color: '#4B5563', marginBottom: 16 }}>
                {isOffline
                  ? "Offline: enter a permit number to look up from local cache."
                  : "Validate permit and sales floor to unlock the workflow."}
              </ThemedText>

              <SelectField
                label={loadingSalesfloors ? "Sales Floor * (Loading…)" : "Sales Floor *"}
                value={salesfloorId}
                onChange={setSalesfloorId}
                options={sfOptions}
                disabled={isPermitValidated}
              />
              {!!salesfloorFetchStatus && sfOptions.length === 0 && (
                <ThemedText style={{ fontSize: 12, color: "#6B7280", marginTop: -6 }}>
                  Portal response: HTTP {salesfloorFetchStatus}
                  {salesfloorFetchError ? ` • ${String(salesfloorFetchError).slice(0, 120)}` : ""}
                </ThemedText>
              )}

              <View style={{ flexDirection: 'row', alignItems: 'flex-end', gap: 10, marginBottom: 12 }}>
                <View style={{ flex: 1 }}>
                  <TextField
                    label={isOffline ? "Permit Number * (offline lookup)" : "Permit QR Token *"}
                    value={permitToken}
                    onChangeText={setPermitToken}
                    placeholder="Scan or manually input…"
                    editable={!isPermitValidated}
                  />
                </View>
                <Pressable style={styles.scanBtnMini} onPress={() => openScanner('permit')} disabled={isPermitValidated}>
                  <MaterialIcons name="qr-code-scanner" size={22} color="#0B6B3A" />
                </Pressable>
              </View>

              {!isPermitValidated ? (
                <ActionButton
                  title={isValidating ? "Validating…" : isOffline ? "Look Up in Cache" : "Validate Permit Token"}
                  onPress={handleVerify}
                  disabled={isValidating}
                  loading={isValidating}
                />
              ) : (
                <SecondaryButton title="Change Permit" icon="edit" onPress={clearValidatedPermit} />
              )}

              {permitStatusText ? (
                <ThemedText style={{ fontSize: 12, marginTop: 10, color: isPermitValidated ? (permitFromCache ? "#92400E" : "#0B6B3A") : "#6B7280" }}>
                  {permitStatusText}
                </ThemedText>
              ) : null}

              {validatedPermit && validatedGrower && (
                <View style={styles.permitInfoPanel}>
                  <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Grower:</ThemedText><ThemedText style={styles.infoValue}>{validatedGrower.display_name} ({validatedGrower.tbz_id})</ThemedText></View>
                  <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Permit:</ThemedText><ThemedText style={styles.infoValue}>{validatedPermit.permit_number}</ThemedText></View>
                  <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Province:</ThemedText><ThemedText style={styles.infoValue}>{province}</ThemedText></View>
                  <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>District:</ThemedText><ThemedText style={styles.infoValue}>{district}</ThemedText></View>
                  <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Remaining Bales:</ThemedText><ThemedText style={[styles.infoValue, { color: '#0B6B3A', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>{validatedPermit.remaining_bales}</ThemedText></View>
                  <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Remaining Weight:</ThemedText><ThemedText style={[styles.infoValue, { color: '#0B6B3A', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>{validatedPermit.remaining_weight_kg} kg</ThemedText></View>
                </View>
              )}

              {validatedPermit && (
                <View style={styles.wizardNav}>
                  <ActionButton title="Next: Batch Info" onPress={moveNext} />
                </View>
              )}
            </View>
          )}

          {/* STEP 2: BATCH DETAILS */}
          {step === 2 && (
            <View style={styles.wizardCard}>
              <View style={styles.wizardCardTitleBar}>
                <MaterialIcons name="inventory" size={20} color="#0B6B3A" style={{ marginRight: 8 }} />
                <ThemedText type="defaultSemiBold">Step 2 - Create Batch Details</ThemedText>
              </View>
              <TextField label="Province *" value={province} onChangeText={setProvince} editable={false} />
              <TextField label="District *" value={district} onChangeText={setDistrict} editable={false} />
              <SelectField label="Buyer / Company" value={buyerId} onChange={setBuyerId} options={buyerOptions} />
              <SelectField label="Tobacco Type *" value={tobaccoType} onChange={setTobaccoType} options={TOBACCO_TYPES.map(t => t.label)} />
              <TextField label="Season *" value={season} onChangeText={setSeason} />
              <TextField label="Sale Date (YYYY-MM-DD) *" value={saleDate} onChangeText={setSaleDate} />
              <TextField label="Grower Representative Name" value={growerRepName} onChangeText={setGrowerRepName} />
              <View style={[styles.wizardNav, { flexDirection: 'row', justifyContent: 'space-between', gap: 12 }]}>
                <View style={{ flex: 1 }}>
                  <SecondaryButton title="Back" icon="chevron-left" onPress={() => setStep(1)} />
                </View>
                <View style={{ flex: 1 }}>
                  <ActionButton title="Continue" onPress={moveNext} />
                </View>
              </View>
            </View>
          )}

          {/* STEP 3: CAPTURE BALES */}
          {step === 3 && (
            <View style={styles.wizardCard}>
              <View style={styles.wizardCardTitleBar}>
                <MaterialIcons name="archive" size={20} color="#0B6B3A" style={{ marginRight: 8 }} />
                <ThemedText type="defaultSemiBold" style={{ flex: 1 }}>Step 3 - Capture Bales</ThemedText>
                <View style={{ backgroundColor: '#0B6B3A', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 12 }}>
                  <ThemedText style={{ color: '#FFFFFF', fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>{bales.length} bales</ThemedText>
                </View>
              </View>
              <ThemedText style={{ fontSize: 13, marginBottom: 16 }}>Bale Details (All tied to validated permit)</ThemedText>

              {bales.map((b, index) => (
                <View key={b.id} style={styles.baleRow}>
                  <ThemedText style={{ fontSize: 11, color: '#6B7280', marginBottom: 4 }}>Row #{index + 1}</ThemedText>
                  <View style={{ flexDirection: 'row', gap: 8, marginBottom: 6 }}>
                    <View style={{ flex: 2 }}>
                      <TextField label="Ticket No. *" value={b.ticket} onChangeText={(v) => updateBale(b.id, 'ticket', v)} />
                    </View>
                    <Pressable style={styles.scanBtnMini} onPress={() => openScanner('bale', b.id)}>
                      <MaterialIcons name="qr-code-scanner" size={20} color="#0B6B3A" />
                    </Pressable>
                    <View style={{ flex: 1.5 }}>
                      <TextField label="Grade *" value={b.grade} onChangeText={(v) => updateBale(b.id, 'grade', v)} />
                    </View>
                  </View>
                  <View style={{ flexDirection: 'row', gap: 8 }}>
                    <View style={{ flex: 1 }}><TextField label="Wt (kg) *" value={b.weight} onChangeText={(v) => updateBale(b.id, 'weight', v)} keyboardType="decimal-pad" /></View>
                    <View style={{ flex: 1 }}><TextField label="Moist %" value={b.moisture} onChangeText={(v) => updateBale(b.id, 'moisture', v)} keyboardType="decimal-pad" /></View>
                    <View style={{ flex: 1 }}><TextField label="Price" value={b.price} onChangeText={(v) => updateBale(b.id, 'price', v)} keyboardType="decimal-pad" /></View>
                    <Pressable onPress={() => removeBale(b.id)} style={styles.deleteBtn}>
                      <MaterialIcons name="delete" size={20} color="#DC2626" />
                    </Pressable>
                  </View>
                  <SelectField label="Outcome *" value={b.outcome} onChange={(v) => updateBale(b.id, 'outcome', v)} options={BALE_OUTCOME_OPTIONS} />
                  {b.outcome === "REJECTED" ? (
                    <SelectField label="Rejection Reason *" value={b.rejectionReason} onChange={(v) => updateBale(b.id, 'rejectionReason', v)} options={BALE_REJECTION_REASON_OPTIONS} required />
                  ) : null}
                  {index < bales.length - 1 && <View style={styles.rowDivider} />}
                </View>
              ))}

              <SecondaryButton title="Add Bale Row" icon="add-circle-outline" onPress={addBale} />

              <View style={[styles.wizardNav, { flexDirection: 'row', justifyContent: 'space-between', gap: 12 }]}>
                <View style={{ flex: 1 }}>
                  <SecondaryButton title="Back" icon="chevron-left" onPress={() => setStep(2)} />
                </View>
                <View style={{ flex: 1 }}>
                  <ActionButton
                    title={isSubmitting ? "Wait…" : isOffline ? "Save Offline" : "Submit Batch"}
                    onPress={handleSubmit}
                    disabled={isSubmitting}
                    loading={isSubmitting}
                  />
                </View>
              </View>
            </View>
          )}

          {/* RECENT CAPTURES */}
          <View style={styles.wizardCard}>
            <View style={styles.wizardCardTitleBar}>
              <MaterialIcons name="history" size={20} color="#374151" style={{ marginRight: 8 }} />
              <ThemedText type="defaultSemiBold" style={{ flex: 1 }}>Today&apos;s Captures</ThemedText>
              {pendingSalesCount > 0 && (
                <Pressable onPress={() => router.push("/pending-sales" as any)}>
                  <ThemedText style={{ fontSize: 12, color: '#0B6B3A' }}>{pendingSalesCount} offline pending →</ThemedText>
                </Pressable>
              )}
            </View>
            {recentBales.length === 0 ? (
              <ThemedText style={{ fontSize: 13, color: '#6B7280', padding: 8 }}>
                {isOffline ? "Cannot load recent captures while offline." : "No bales captured today yet."}
              </ThemedText>
            ) : (
              <View style={styles.tableBlock}>
                {recentBales.map((rb, idx) => (
                  <View key={rb.id} style={[styles.tableRow, idx > 0 && { borderTopWidth: 1, borderColor: '#F3F4F6' }]}>
                    <View style={{ flex: 1 }}>
                      <ThemedText style={{ fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>{rb.bale_ticket_number}</ThemedText>
                      <ThemedText style={{ fontSize: 10, color: '#6B7280' }} numberOfLines={1}>{rb.grower_name}</ThemedText>
                    </View>
                    <View style={{ alignItems: 'flex-end' }}>
                      <ThemedText style={{ fontSize: 12 }}>{parseFloat(rb.weight_kg).toFixed(1)}kg</ThemedText>
                      <ThemedText style={{ fontSize: 10, color: rb.status === 'BOUGHT' ? '#0B6B3A' : '#DC2626', fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>{rb.status}</ThemedText>
                    </View>
                  </View>
                ))}
              </View>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F9FAFB' },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingBottom: 16,
    backgroundColor: '#0B6B3A',
    borderBottomWidth: 0,
  },
  offlineBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#374151',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  offlineBannerText: { color: '#FFFFFF', fontSize: 12, flex: 1 },
  cacheWarning: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#FDE68A',
  },
  cacheWarningText: { color: '#92400E', fontSize: 12, flex: 1 },
  pendingBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: '#0B6B3A',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
  },
  pendingBadgeText: { color: '#FFFFFF', fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  stepperContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    backgroundColor: '#FFFFFF',
  },
  stepDot: { width: 28, height: 28, borderRadius: 14, backgroundColor: '#F3F4F6', alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#D1D5DB' },
  stepDotActive: { backgroundColor: '#0B6B3A', borderColor: '#0B6B3A' },
  stepDotText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: '#6B7280' },
  stepDotTextActive: { color: '#FFFFFF' },
  stepLine: { width: 40, height: 2, backgroundColor: '#E5E7EB', marginHorizontal: 8 },
  stepLineActive: { backgroundColor: '#0B6B3A' },
  content: { padding: 16, paddingBottom: 60, gap: 16 },
  wizardCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E5E7EB',
    shadowColor: '#000',
    shadowOpacity: 0.02,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  wizardCardTitleBar: { flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: '#F3F4F6', paddingBottom: 12, marginBottom: 16 },
  scanBtnMini: {
    height: 52,
    width: 52,
    borderRadius: 26,
    borderWidth: 1,
    borderColor: "#A7F3D0",
    backgroundColor: "#ECFDF5",
    alignItems: "center",
    justifyContent: "center",
  },
  permitInfoPanel: { backgroundColor: '#E8F3EE', borderRadius: 8, padding: 12, marginTop: 8, gap: 6 },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  infoLabel: { fontSize: 12, color: '#374151' },
  infoValue: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: '#111827' },
  wizardNav: { marginTop: 18, borderTopWidth: 1, borderTopColor: '#F3F4F6', paddingTop: 16, paddingBottom: 2 },
  actionBtn: {
    height: 56,
    borderRadius: 28,
    backgroundColor: "#0B6B3A",
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 10,
    paddingHorizontal: 16,
    shadowColor: "#000",
    shadowOpacity: 0.12,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 6 },
    elevation: 3,
  },
  actionBtnPressed: { opacity: 0.92 },
  actionBtnDisabled: { backgroundColor: "#A7B3AB", shadowOpacity: 0, elevation: 0 },
  actionBtnText: {
    color: "#FFFFFF",
    fontSize: 13,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    lineHeight: 16,
  },
  secondaryBtn: {
    height: 56,
    borderRadius: 28,
    borderWidth: 1,
    borderColor: "#A7F3D0",
    backgroundColor: "#ECFDF5",
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
    paddingHorizontal: 14,
  },
  secondaryBtnPressed: { opacity: 0.92 },
  secondaryBtnDisabled: { opacity: 0.6 },
  secondaryBtnText: {
    color: "#0B6B3A",
    fontSize: 13,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    lineHeight: 16,
  },
  baleRow: { marginBottom: 16 },
  rowDivider: { height: 1, backgroundColor: '#E5E7EB', marginTop: 16 },
  deleteBtn: { width: 44, height: 48, marginTop: 28, borderRadius: 8, backgroundColor: '#FEF2F2', alignItems: 'center', justifyContent: 'center' },
  tableBlock: { backgroundColor: '#FFFFFF', borderRadius: 8, borderWidth: 1, borderColor: '#F3F4F6' },
  tableRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, paddingHorizontal: 12 },
  scannerWrapper: { flex: 1, backgroundColor: '#000000' },
  scannerHeader: { position: 'absolute', top: 50, left: 20, right: 20, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', zIndex: 10, backgroundColor: 'rgba(0,0,0,0.5)', padding: 12, borderRadius: 12 },
  scannerOverlay: { ...StyleSheet.absoluteFillObject, justifyContent: 'center', alignItems: 'center', zIndex: 5 },
  scannerCutout: { width: 250, height: 250, borderWidth: 2, borderColor: '#0B6B3A', backgroundColor: 'transparent', borderRadius: 12 },
});
