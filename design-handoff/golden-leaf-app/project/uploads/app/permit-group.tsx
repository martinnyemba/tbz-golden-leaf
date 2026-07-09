import DateTimePicker from "@react-native-community/datetimepicker";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { CameraView, type BarcodeScanningResult, useCameraPermissions } from "expo-camera";
import { useFocusEffect, useRouter } from "expo-router";
import React, { useCallback, useMemo, useState } from "react";
import { Alert, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, TextInput, View } from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson } from "@/lib/inspection-storage";
import { readCacheItems, writeCache } from "@/lib/offline-cache";

const GROWER_CATEGORIES = ["Small Scale", "Commercial", "Company"];
const PURPOSES = ["Sales", "Processing", "Storage", "Export"];
const PROVINCES = ["Central", "Copperbelt", "Eastern", "Luapula", "Lusaka", "Muchinga", "Northern", "North-Western", "Southern", "Western"];
const DISTRICTS: Record<string, string[]> = {
  Central: ["Chibombo", "Chisamba", "Chitambo", "Itezhi-Tezhi", "Kabwe", "Kapiri Mposhi", "Luano", "Mkushi", "Mumbwa", "Ngabwe", "Serenje", "Shibuyunji"],
  Copperbelt: ["Chililabombwe", "Chingola", "Kalulushi", "Kitwe", "Luanshya", "Lufwanyama", "Masaiti", "Mpongwe", "Mufulira", "Ndola"],
  Eastern: ["Chadiza", "Chama", "Chasefu", "Chipangali", "Chipata", "Kasenengwa", "Katete", "Lumezi", "Lundazi", "Mambwe", "Nyimba", "Petauke", "Sinda", "Vubwi"],
  Luapula: ["Chembe", "Chienge", "Chifunabuli", "Kawambwa", "Lunga", "Mansa", "Milenge", "Mwansabombwe", "Mwense", "Nchelenge", "Samfya"],
  Lusaka: ["Chilanga", "Chongwe", "Kafue", "Luangwa", "Lusaka", "Rufunsa"],
  Muchinga: ["Chama", "Chinsali", "Isoka", "Kanchibiya", "Lavushimanda", "Mafinga", "Mpika", "Nakonde", "Shiwa Ng'andu"],
  Northern: ["Chilubi", "Kaputa", "Kasama", "Lunte", "Luwingu", "Mbala", "Mporokoso", "Mpulungu", "Mungwi", "Nsama", "Senga Hill"],
  "North-Western": ["Chavuma", "Ikelenge", "Kabompo", "Kalumbila", "Kasempa", "Manyinga", "Mufumbwe", "Mushindamo", "Mwinilunga", "Solwezi", "Zambezi"],
  Southern: ["Chikankata", "Choma", "Gwembe", "Kalomo", "Kazungula", "Livingstone", "Mazabuka", "Monze", "Namwala", "Pemba", "Siavonga", "Sinazongwe", "Zimba"],
  Western: ["Kalabo", "Kaoma", "Limulunga", "Luampa", "Lukulu", "Mitete", "Mongu", "Mulobezi", "Mwandi", "Nalolo", "Nkeyema", "Senanga", "Sesheke", "Shang'ombo", "Sikongo", "Sioma"],
};

type ApiListResponse<T> = { results?: T[]; next?: string | null; count?: number } | T[];

type GroupPermitEntry = {
  id: string;
  entry_number?: string | null;
  sub_reference?: string | null;
  grower: string;
  grower_name?: string;
  grower_tbz_id?: string;
  grower_category: string;
  total_bales: number;
  total_weight_kg: string | number;
  used_bales?: number;
  used_weight_kg?: string | number;
  remaining_bales?: number;
  remaining_weight_kg?: string | number;
  notes?: string;
  status: string;
  created_at?: string;
  updated_at?: string;
};

type GroupPermit = {
  id: string;
  group_permit_number?: string | null;
  license_plate: string;
  origin_province: string;
  origin_district: string;
  destination_salesfloor: string;
  purpose: string;
  valid_from?: string | null;
  valid_to?: string | null;
  days_valid?: number | null;
  total_bales: number;
  total_weight_kg: string | number;
  status: string;
  comments?: string;
  rejection_reason?: string;
  qr_code_data?: string;
  entries?: GroupPermitEntry[];
  created_at?: string;
  updated_at?: string;
};

type Mode = "list" | "create_header" | "create_entries" | "detail" | "validate" | "approve";

function listFromApi<T>(payload: ApiListResponse<T>): { items: T[]; next: string; count?: number } {
  if (Array.isArray(payload)) return { items: payload, next: "" };
  const items = Array.isArray(payload?.results) ? payload.results : [];
  const next = typeof payload?.next === "string" ? payload.next : "";
  const count = typeof payload?.count === "number" ? payload.count : undefined;
  return { items, next, count };
}

function safeJson(raw: string) {
  try {
    return { ok: true as const, value: JSON.parse(raw) };
  } catch (e) {
    const msg = e instanceof Error ? e.message : "Invalid JSON";
    return { ok: false as const, error: msg };
  }
}

function statusBadgeColors(status: string) {
  const s = String(status || "").toUpperCase();
  if (s === "APPROVED") return { bg: "#E8F3EE", text: "#0B6B3A" };
  if (s === "PENDING") return { bg: "#FEF3C7", text: "#D97706" };
  if (s === "DRAFT") return { bg: "#E5E7EB", text: "#374151" };
  return { bg: "#FEE2E2", text: "#B91C1C" };
}

function toYmd(d: Date) {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDate(raw?: string | null) {
  if (!raw) return "—";
  const parts = String(raw).slice(0, 10);
  return parts;
}

type PermitGroupFlowProps = {
  initialMode?: Mode;
  initialStatus?: string;
  lockedStatus?: string;
  showDashboard?: boolean;
  showStatusPills?: boolean;
  exitMode?: "internal" | "back";
  listSubtitle?: string;
};

export default function PermitGroupScreen({
  initialMode = "list",
  initialStatus = "",
  lockedStatus,
  showDashboard = true,
  showStatusPills = false,
  exitMode = "internal",
  listSubtitle,
}: PermitGroupFlowProps = {}) {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const primary = Colors[theme].primary;

  const [mode, setMode] = useState<Mode>(initialMode);
  const [selectedStatus, setSelectedStatus] = useState<string>(initialStatus);
  const [canApprovePermits, setCanApprovePermits] = useState(false);

  const [kpiDraft, setKpiDraft] = useState<number | null>(null);
  const [kpiPending, setKpiPending] = useState<number | null>(null);
  const [kpiActive, setKpiActive] = useState<number | null>(null);

  const [groups, setGroups] = useState<GroupPermit[]>([]);
  const [groupsLoading, setGroupsLoading] = useState(false);
  const [groupsNext, setGroupsNext] = useState<string>("");

  const [salesfloors, setSalesfloors] = useState<any[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);

  const [selectedGp, setSelectedGp] = useState<GroupPermit | null>(null);

  const [licensePlate, setLicensePlate] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");
  const [destinationSalesfloor, setDestinationSalesfloor] = useState("");
  const [purpose, setPurpose] = useState("");
  const [comments, setComments] = useState("");
  const [creating, setCreating] = useState(false);

  const [entries, setEntries] = useState<GroupPermitEntry[]>([]);
  const [entriesLoading, setEntriesLoading] = useState(false);
  const [entrySubmitting, setEntrySubmitting] = useState(false);
  const [growerSearch, setGrowerSearch] = useState("");
  const [growerResults, setGrowerResults] = useState<any[]>([]);
  const [growerSearching, setGrowerSearching] = useState(false);
  const [entryGrower, setEntryGrower] = useState<any>(null);
  const [entryCategory, setEntryCategory] = useState("");
  const [entryBales, setEntryBales] = useState("");
  const [entryWeight, setEntryWeight] = useState("");
  const [entryNotes, setEntryNotes] = useState("");

  const [permission, requestPermission] = useCameraPermissions();
  const [scannerOpen, setScannerOpen] = useState(false);
  const [scanned, setScanned] = useState(false);
  const [validateToken, setValidateToken] = useState("");
  const [validating, setValidating] = useState(false);
  const [validateResult, setValidateResult] = useState<any>(null);

  const [reviewAction, setReviewAction] = useState<"approve" | "reject">("approve");
  const [validFrom, setValidFrom] = useState<Date>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const [validTo, setValidTo] = useState<Date>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 2);
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const [showValidFromPicker, setShowValidFromPicker] = useState(false);
  const [showValidToPicker, setShowValidToPicker] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [approving, setApproving] = useState(false);

  const districts = useMemo(() => (province ? DISTRICTS[province] ?? [] : []), [province]);
  const sfOptions = useMemo(
    () =>
      salesfloors
        .map((sf) => String(sf?.name ?? sf?.salesfloor_name ?? sf?.legal_name ?? sf?.company_name ?? "").trim())
        .filter(Boolean),
    [salesfloors],
  );

  const statusPills = useMemo(
    () => [
      { value: "", label: "All" },
      { value: "DRAFT", label: "Draft" },
      { value: "PENDING", label: "Pending Review" },
      { value: "APPROVED", label: "Approved" },
      { value: "REJECTED", label: "Rejected" },
      { value: "USED", label: "Used" },
      { value: "EXPIRED", label: "Expired" },
      { value: "RETURNED", label: "Returned" },
    ],
    [],
  );

  const listSubtitleText = useMemo(() => {
    if (listSubtitle) return listSubtitle;
    if (showDashboard) return "Multi-grower movement permits — one vehicle, multiple growers";
    const pill = statusPills.find((p) => p.value === selectedStatus);
    if (pill && pill.value) return `${pill.label} Group Permits`;
    return "Group Permits";
  }, [listSubtitle, selectedStatus, showDashboard, statusPills]);

  const resetCreate = useCallback(() => {
    setSelectedGp(null);
    setLicensePlate("");
    setProvince("");
    setDistrict("");
    setDestinationSalesfloor("");
    setPurpose("");
    setComments("");
    setEntries([]);
    setEntryGrower(null);
    setEntryCategory("");
    setEntryBales("");
    setEntryWeight("");
    setEntryNotes("");
    setGrowerSearch("");
    setGrowerResults([]);
  }, []);

  const fetchMe = useCallback(async () => {
    const resp = await apiFetchJson("/api/v1/mobile/me/", { method: "GET" });
    if (!resp.ok) return;
    const parsed = safeJson(resp.body);
    if (!parsed.ok) return;
    const perms = parsed.value?.permissions ?? {};
    setCanApprovePermits(Boolean(perms?.can_approve_permits));
  }, []);

  const fetchCounts = useCallback(async () => {
    async function countFor(status: string) {
      const path = status ? `/api/v1/permits/group-permits/?status=${encodeURIComponent(status)}&page_size=1` : "/api/v1/permits/group-permits/?page_size=1";
      const resp = await apiFetchJson(path, { method: "GET" });
      if (!resp.ok) return null;
      const parsed = safeJson(resp.body);
      if (!parsed.ok) return null;
      const { count } = listFromApi<any>(parsed.value);
      return typeof count === "number" ? count : null;
    }

    const [draft, pending, approved] = await Promise.all([countFor("DRAFT"), countFor("PENDING"), countFor("APPROVED")]);
    setKpiDraft(draft);
    setKpiPending(pending);
    setKpiActive(approved);
  }, []);

  const fetchSalesfloors = useCallback(async () => {
    setOptionsLoading(true);
    try {
      const cachedSf = await readCacheItems<any>("salesfloors");
      if (cachedSf.length > 0 && salesfloors.length === 0) setSalesfloors(cachedSf);
      const resp = await apiFetchJson("/api/v1/entities/legal-entities/?entity_type=SALES_FLOOR&page_size=250", { method: "GET" });
      if (resp.ok) {
        const parsed = safeJson(resp.body);
        if (parsed.ok) {
          const rows = Array.isArray(parsed.value) ? parsed.value : parsed.value?.results ?? [];
          setSalesfloors(rows);
          if (rows.length > 0) void writeCache("salesfloors", rows);
        }
      }
    } finally {
      setOptionsLoading(false);
    }
  }, [salesfloors.length]);

  const fetchGroupPermits = useCallback(
    async (opts?: { reset?: boolean; status?: string }) => {
      if (groupsLoading) return;
      setGroupsLoading(true);
      try {
        const nextPath = opts?.reset ? "" : groupsNext;
        const effectiveStatus = typeof opts?.status === "string" ? opts.status : selectedStatus;
        const basePath = effectiveStatus
          ? `/api/v1/permits/group-permits/?status=${encodeURIComponent(effectiveStatus)}`
          : "/api/v1/permits/group-permits/";
        const path = nextPath || `${basePath}${basePath.includes("?") ? "&" : "?"}page_size=20`;
        const resp = await apiFetchJson(path, { method: "GET" });
        if (!resp.ok) {
          const ui = apiErrorUi(resp.status, resp.body);
          Alert.alert(ui.title, ui.message);
          return;
        }
        const parsed = safeJson(resp.body);
        if (!parsed.ok) throw new Error(parsed.error);
        const { items, next } = listFromApi<GroupPermit>(parsed.value);
        setGroupsNext(next ? (() => {
          try {
            const u = new URL(next);
            return `${u.pathname}${u.search}`;
          } catch {
            return next;
          }
        })() : "");
        setGroups((prev) => (opts?.reset ? items : prev.concat(items)));
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Network request failed.";
        Alert.alert("Network error", msg);
      } finally {
        setGroupsLoading(false);
      }
    },
    [groupsLoading, groupsNext, selectedStatus],
  );

  const openList = useCallback(() => {
    if (exitMode === "back") {
      router.back();
      return;
    }
    setMode("list");
    setSelectedGp(null);
    setValidateResult(null);
    setValidateToken("");
    setScanned(false);
  }, [exitMode, router]);

  const openValidate = useCallback(() => {
    setMode("validate");
    setValidateResult(null);
    setScanned(false);
  }, []);

  const openCreate = useCallback(() => {
    router.push("/permit-group-create" as any);
  }, [router]);

  const openDetail = useCallback((gp: GroupPermit) => {
    router.push({ pathname: "/permit-group-detail" as any, params: { id: gp.id } } as any);
  }, [router]);

  const openEntries = useCallback(
    async (gp: GroupPermit) => {
      setSelectedGp(gp);
      setMode("create_entries");
      setEntries([]);
      setEntriesLoading(true);
      try {
        const resp = await apiFetchJson(`/api/v1/permits/group-permits/${gp.id}/entries/`, { method: "GET" });
        if (!resp.ok) {
          const ui = apiErrorUi(resp.status, resp.body);
          Alert.alert(ui.title, ui.message);
          return;
        }
        const parsed = safeJson(resp.body);
        if (!parsed.ok) throw new Error(parsed.error);
        setEntries(Array.isArray(parsed.value) ? parsed.value : []);
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Failed to load entries.";
        Alert.alert("Error", msg);
      } finally {
        setEntriesLoading(false);
      }
    },
    [],
  );

  const openApprove = useCallback((gp: GroupPermit) => {
    setSelectedGp(gp);
    setReviewAction("approve");
    setRejectReason("");
    const d1 = new Date();
    d1.setDate(d1.getDate() + 1);
    d1.setHours(0, 0, 0, 0);
    const d2 = new Date();
    d2.setDate(d2.getDate() + 2);
    d2.setHours(0, 0, 0, 0);
    setValidFrom(d1);
    setValidTo(d2);
    setMode("approve");
  }, []);

  useFocusEffect(
    useCallback(() => {
      void fetchMe();
      if (showDashboard) void fetchCounts();
      if (mode === "list" && !showDashboard) {
        const effectiveStatus = typeof lockedStatus === "string" ? lockedStatus : selectedStatus;
        if (typeof lockedStatus === "string" && lockedStatus !== selectedStatus) {
          setSelectedStatus(lockedStatus);
        }
        setGroups([]);
        setGroupsNext("");
        void fetchGroupPermits({ reset: true, status: effectiveStatus });
      }
      if (mode === "create_header" || mode === "create_entries") {
        void fetchSalesfloors();
      }
    }, [fetchCounts, fetchGroupPermits, fetchMe, fetchSalesfloors, lockedStatus, mode, selectedStatus, showDashboard]),
  );

  const onChangeStatus = useCallback(
    (status: string) => {
      setSelectedStatus(status);
      setGroups([]);
      setGroupsNext("");
      void fetchGroupPermits({ reset: true, status });
    },
    [fetchGroupPermits],
  );

  const createGroupPermit = async () => {
    if (creating) return;
    const missing: string[] = [];
    if (!licensePlate.trim()) missing.push("License Plate");
    if (!province) missing.push("Origin Province");
    if (!district) missing.push("Origin District");
    if (!destinationSalesfloor.trim()) missing.push("Destination Salesfloor");
    if (!purpose) missing.push("Purpose");
    if (missing.length) {
      Alert.alert("Missing Information", `Please complete:\n${missing.join("\n")}`);
      return;
    }
    setCreating(true);
    try {
      const purposeMap: Record<string, string> = { Sales: "SALES", Processing: "PROCESSING", Storage: "STORAGE", Export: "EXPORT" };
      const payload = {
        license_plate: licensePlate.trim(),
        origin_province: province.toUpperCase().replace("-", "_"),
        origin_district: district,
        destination_salesfloor: destinationSalesfloor.trim(),
        purpose: purposeMap[purpose] || "SALES",
        comments: comments.trim(),
      };
      const resp = await apiFetchJson("/api/v1/permits/group-permits/", { method: "POST", body: JSON.stringify(payload) });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        Alert.alert(ui.title, ui.message);
        return;
      }
      const parsed = safeJson(resp.body);
      if (!parsed.ok) throw new Error(parsed.error);
      const gp = parsed.value as GroupPermit;
      setSelectedGp(gp);
      setMode("create_entries");
      setEntries([]);
      setGrowerResults([]);
      setGrowerSearch("");
      setEntryGrower(null);
      setEntryCategory("");
      setEntryBales("");
      setEntryWeight("");
      setEntryNotes("");
      const respEntries = await apiFetchJson(`/api/v1/permits/group-permits/${gp.id}/entries/`, { method: "GET" });
      if (respEntries.ok) {
        const p2 = safeJson(respEntries.body);
        if (p2.ok && Array.isArray(p2.value)) setEntries(p2.value);
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to create group permit.";
      Alert.alert("Error", msg);
    } finally {
      setCreating(false);
    }
  };

  const searchGrowers = async () => {
    if (!growerSearch.trim()) return;
    setGrowerSearching(true);
    try {
      const resp = await apiFetchJson(`/api/v1/growers/growers/?search=${encodeURIComponent(growerSearch.trim())}&page_size=20`, { method: "GET" });
      if (!resp.ok) return;
      const parsed = safeJson(resp.body);
      if (!parsed.ok) return;
      const rows = Array.isArray(parsed.value) ? parsed.value : parsed.value?.results ?? [];
      setGrowerResults(rows);
    } finally {
      setGrowerSearching(false);
    }
  };

  const addEntry = async () => {
    if (!selectedGp) return;
    if (entrySubmitting) return;
    const missing: string[] = [];
    if (!entryGrower?.id) missing.push("Grower");
    if (!entryCategory) missing.push("Grower Category");
    if (!entryBales.trim()) missing.push("Total Bales");
    if (!entryWeight.trim()) missing.push("Weight (kg)");
    if (missing.length) {
      Alert.alert("Missing Information", `Please complete:\n${missing.join("\n")}`);
      return;
    }
    const catMap: Record<string, string> = { "Small Scale": "SMALL_SCALE", Commercial: "COMMERCIAL", Company: "COMPANY" };
    setEntrySubmitting(true);
    try {
      const resp = await apiFetchJson(`/api/v1/permits/group-permits/${selectedGp.id}/entries/`, {
        method: "POST",
        body: JSON.stringify({
          grower: entryGrower.id,
          grower_category: catMap[entryCategory],
          total_bales: parseInt(entryBales.trim(), 10),
          total_weight_kg: entryWeight.trim(),
          notes: entryNotes.trim(),
        }),
      });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        Alert.alert(ui.title, ui.message);
        return;
      }
      const parsed = safeJson(resp.body);
      if (!parsed.ok) throw new Error(parsed.error);
      const newEntry = parsed.value as GroupPermitEntry;
      setEntries((prev) => [newEntry, ...prev]);
      setEntryGrower(null);
      setEntryCategory("");
      setEntryBales("");
      setEntryWeight("");
      setEntryNotes("");
      setGrowerResults([]);
      setGrowerSearch("");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to add entry.";
      Alert.alert("Error", msg);
    } finally {
      setEntrySubmitting(false);
    }
  };

  const removeEntry = async (entryId: string) => {
    if (!selectedGp) return;
    const resp = await apiFetchJson(`/api/v1/permits/group-permits/${selectedGp.id}/remove-entry/`, {
      method: "POST",
      body: JSON.stringify({ entry_id: entryId }),
    });
    if (!resp.ok) {
      const ui = apiErrorUi(resp.status, resp.body);
      Alert.alert(ui.title, ui.message);
      return;
    }
    setEntries((prev) => prev.filter((e) => e.id !== entryId));
  };

  const submitGroupPermit = async () => {
    if (!selectedGp) return;
    if (entries.length < 2) {
      Alert.alert("Not enough entries", "A group permit must contain at least 2 growers.");
      return;
    }
    const resp = await apiFetchJson(`/api/v1/permits/group-permits/${selectedGp.id}/submit/`, { method: "POST" });
    if (!resp.ok) {
      const ui = apiErrorUi(resp.status, resp.body);
      Alert.alert(ui.title, ui.message);
      return;
    }
    const parsed = safeJson(resp.body);
    if (parsed.ok) setSelectedGp(parsed.value as GroupPermit);
    Alert.alert("Submitted", "Group permit submitted for inspector review.", [{ text: "OK", onPress: openList }]);
  };

  const validateGroupToken = async () => {
    if (validating) return;
    if (!validateToken.trim()) {
      Alert.alert("Missing token", "Scan or paste the group permit QR token to validate.");
      return;
    }
    setValidating(true);
    try {
      const resp = await apiFetchJson("/api/v1/permits/group-permits/validate-qr/", { method: "POST", body: JSON.stringify({ permit_token: validateToken.trim() }) });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        Alert.alert(ui.title, ui.message);
        return;
      }
      const parsed = safeJson(resp.body);
      if (!parsed.ok) throw new Error(parsed.error);
      setValidateResult(parsed.value);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Validation failed.";
      Alert.alert("Error", msg);
    } finally {
      setValidating(false);
    }
  };

  const onBarcodeScanned = useCallback(
    (result: BarcodeScanningResult) => {
      if (scanned) return;
      setScanned(true);
      setValidateToken(result.data);
      setScannerOpen(false);
      setTimeout(() => setScanned(false), 800);
    },
    [scanned],
  );

  const submitApproval = async () => {
    if (!selectedGp) return;
    if (approving) return;
    if (reviewAction === "reject" && !rejectReason.trim()) {
      Alert.alert("Missing reason", "Enter a rejection reason.");
      return;
    }
    setApproving(true);
    try {
      const payload =
        reviewAction === "approve"
          ? { action: "approve", valid_from: toYmd(validFrom), valid_to: toYmd(validTo) }
          : { action: "reject", reason: rejectReason.trim() };
      const resp = await apiFetchJson(`/api/v1/permits/group-permits/${selectedGp.id}/approve-reject/`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      if (!resp.ok) {
        const ui = apiErrorUi(resp.status, resp.body);
        Alert.alert(ui.title, ui.message);
        return;
      }
      const parsed = safeJson(resp.body);
      if (parsed.ok) setSelectedGp(parsed.value as GroupPermit);
      Alert.alert("Done", "Group permit updated.", [{ text: "OK", onPress: openList }]);
    } finally {
      setApproving(false);
    }
  };

  const renderHeader = (subtitle: string) => (
    <View style={styles.header}>
      <View style={{ flex: 1 }}>
        <ThemedText style={styles.title}>Group Permits</ThemedText>
        <ThemedText style={styles.subtitle}>{subtitle}</ThemedText>
      </View>
      <Pressable onPress={() => router.back()} style={styles.closeBtn}>
        <MaterialIcons name="close" size={22} color="#6B7280" />
      </Pressable>
    </View>
  );

  const renderActionsRow = () => (
    <View style={styles.actionsRow}>
      <Pressable
        onPress={openValidate}
        style={({ pressed }) => [
          styles.actionBtn,
          styles.actionBtnOutline,
          { borderColor: primary },
          pressed && styles.actionBtnOutlinePressed,
        ]}
      >
        <MaterialIcons name="qr-code-scanner" size={18} color={primary} />
        <ThemedText style={[styles.actionBtnTextOutline, { color: primary }]} numberOfLines={1} ellipsizeMode="tail">
          Validate
        </ThemedText>
      </Pressable>
      <Pressable
        onPress={openCreate}
        style={({ pressed }) => [styles.actionBtn, styles.actionBtnPrimary, { backgroundColor: primary }, pressed && styles.actionBtnPrimaryPressed]}
      >
        <MaterialIcons name="groups" size={18} color="#FFFFFF" />
        <ThemedText style={styles.actionBtnTextPrimary} numberOfLines={1} ellipsizeMode="tail">
          New Group Permit
        </ThemedText>
      </Pressable>
    </View>
  );

  if (mode === "create_header") {
    return (
      <ThemedView style={styles.container}>
        <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            {renderHeader("Step 1 of 2 — Vehicle & Route")}
            <View style={styles.card}>
              <TextField label="License Plate" value={licensePlate} onChangeText={setLicensePlate} placeholder="e.g. ABC 1234" required />
              <SelectField label="Origin Province" value={province} onChange={setProvince} options={PROVINCES} required />
              <SelectField label="Origin District" value={district} onChange={setDistrict} options={districts} required />
              {optionsLoading ? <View style={{ marginTop: 10, alignItems: "center" }}><LeafLoader size={22} /></View> : null}
              <SelectField label="Destination Salesfloor" value={destinationSalesfloor} onChange={setDestinationSalesfloor} options={sfOptions} required />
              <SelectField label="Purpose" value={purpose} onChange={setPurpose} options={PURPOSES} required />
              <TextField label="Comments" value={comments} onChangeText={setComments} placeholder="Optional" multiline />
              <View style={{ marginTop: 14 }}>
                <View style={styles.actionsRow}>
                  <Pressable
                    onPress={openList}
                    style={({ pressed }) => [
                      styles.actionBtn,
                      styles.actionBtnOutline,
                      { borderColor: primary },
                      pressed && styles.actionBtnOutlinePressed,
                    ]}
                  >
                    <MaterialIcons name="close" size={18} color={primary} />
                    <ThemedText style={[styles.actionBtnTextOutline, { color: primary }]} numberOfLines={1} ellipsizeMode="tail">
                      Cancel
                    </ThemedText>
                  </Pressable>
                  <Pressable
                    onPress={createGroupPermit}
                    disabled={creating}
                    style={({ pressed }) => [
                      styles.actionBtn,
                      styles.actionBtnPrimary,
                      { backgroundColor: primary, opacity: creating ? 0.7 : 1 },
                      pressed && !creating && styles.actionBtnPrimaryPressed,
                    ]}
                  >
                    {creating ? <LeafLoader size={18} color="#FFFFFF" /> : <MaterialIcons name="arrow-forward" size={18} color="#FFFFFF" />}
                    <ThemedText style={styles.actionBtnTextPrimary} numberOfLines={1} ellipsizeMode="tail">
                      Continue
                    </ThemedText>
                  </Pressable>
                </View>
              </View>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </ThemedView>
    );
  }

  if (mode === "create_entries" && selectedGp) {
    return (
      <ThemedView style={styles.container}>
        <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            {renderHeader("Step 2 of 2 — Add Growers")}
            <View style={styles.card}>
              <ThemedText style={styles.cardTitle}>Group</ThemedText>
              <ThemedText style={styles.cardMeta}>{selectedGp.license_plate} → {selectedGp.destination_salesfloor}</ThemedText>
              <ThemedText style={styles.cardMeta}>Purpose: {selectedGp.purpose}</ThemedText>
            </View>

            <View style={styles.card}>
              <ThemedText style={styles.cardTitle}>Add Grower Entry</ThemedText>
              <View style={{ flexDirection: "row", gap: 10, alignItems: "flex-end" }}>
                <View style={{ flex: 1 }}>
                  <TextField label="Search grower" value={growerSearch} onChangeText={setGrowerSearch} placeholder="Name, TBZ ID, or NRC..." />
                </View>
                <Pressable
                  onPress={searchGrowers}
                  style={({ pressed }) => [styles.searchBtn, { backgroundColor: primary }, pressed && styles.searchBtnPressed]}
                >
                  {growerSearching ? <LeafLoader size={18} color="#FFFFFF" /> : <MaterialIcons name="search" size={20} color="#FFFFFF" />}
                </Pressable>
              </View>
              {growerResults.length > 0 ? (
                <View style={styles.resultsWrap}>
                  <ScrollView nestedScrollEnabled keyboardShouldPersistTaps="handled" style={{ maxHeight: 240 }}>
                    {growerResults.map((g) => (
                      <Pressable
                        key={String(g.id)}
                        onPress={() => {
                          setEntryGrower(g);
                          setGrowerResults([]);
                          setGrowerSearch(String(g.display_name ?? ""));
                        }}
                        style={styles.resultRow}
                      >
                        <ThemedText style={styles.resultTitle}>{String(g.display_name ?? "").trim()}</ThemedText>
                        <ThemedText style={styles.resultMeta}>{String(g.tbz_id ?? "")} • {String(g.nrc_number ?? "No NRC")}</ThemedText>
                      </Pressable>
                    ))}
                  </ScrollView>
                </View>
              ) : null}

              <SelectField label="Grower Category" value={entryCategory} onChange={setEntryCategory} options={GROWER_CATEGORIES} required />
              <View style={{ flexDirection: "row", gap: 12 }}>
                <View style={{ flex: 1 }}>
                  <TextField label="Total Bales" value={entryBales} onChangeText={setEntryBales} keyboardType="number-pad" required />
                </View>
                <View style={{ flex: 1 }}>
                  <TextField label="Weight (Kg)" value={entryWeight} onChangeText={setEntryWeight} keyboardType="decimal-pad" required />
                </View>
              </View>
              <TextField label="Notes" value={entryNotes} onChangeText={setEntryNotes} placeholder="Optional" multiline />
              <View style={{ marginTop: 12 }}>
                <PrimaryButton title={entrySubmitting ? "Adding..." : "Add Entry"} onPress={addEntry} disabled={entrySubmitting} loading={entrySubmitting} />
              </View>
            </View>

            <View style={styles.card}>
              <ThemedText style={styles.cardTitle}>Grower Manifest ({entries.length})</ThemedText>
              {entriesLoading ? <View style={{ marginTop: 10, alignItems: "center" }}><LeafLoader size={28} /></View> : null}
              {entries.length === 0 && !entriesLoading ? <ThemedText style={styles.cardMeta}>No entries yet.</ThemedText> : null}
              {entries.map((e) => (
                <View key={e.id} style={styles.entryRow}>
                  <View style={{ flex: 1 }}>
                    <ThemedText style={styles.entryTitle}>{e.grower_name || e.grower_tbz_id || "Grower"}</ThemedText>
                    <ThemedText style={styles.entryMeta}>{e.grower_tbz_id || ""} • {e.grower_category}</ThemedText>
                    <ThemedText style={styles.entryMeta}>{e.total_bales} bales • {String(e.total_weight_kg)} kg</ThemedText>
                  </View>
                  <Pressable onPress={() => removeEntry(e.id)} style={styles.removeBtn}>
                    <MaterialIcons name="delete" size={18} color="#DC2626" />
                  </Pressable>
                </View>
              ))}

              <View style={{ marginTop: 14, gap: 10 }}>
                <PrimaryButton title="Submit for Approval" onPress={submitGroupPermit} />
                <PrimaryButton title="Back to List" onPress={openList} />
              </View>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </ThemedView>
    );
  }

  if (mode === "validate") {
    return (
      <ThemedView style={styles.container}>
        <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            {renderHeader("Validate Group Permit")}
            <View style={styles.actionsRow}>
              <Pressable
                onPress={() => {
                  if (!permission?.granted) {
                    void requestPermission();
                    return;
                  }
                  setScannerOpen(true);
                }}
                style={({ pressed }) => [
                  styles.actionBtn,
                  styles.actionBtnOutline,
                  { borderColor: primary },
                  pressed && styles.actionBtnOutlinePressed,
                ]}
              >
                <MaterialIcons name="qr-code-scanner" size={18} color={primary} />
                <ThemedText style={[styles.actionBtnTextOutline, { color: primary }]} numberOfLines={1} ellipsizeMode="tail">
                  Scan QR
                </ThemedText>
              </Pressable>
              <Pressable
                onPress={openList}
                style={({ pressed }) => [styles.actionBtn, styles.actionBtnPrimary, { backgroundColor: primary }, pressed && styles.actionBtnPrimaryPressed]}
              >
                <MaterialIcons name="list" size={18} color="#FFFFFF" />
                <ThemedText style={styles.actionBtnTextPrimary} numberOfLines={1} ellipsizeMode="tail">
                  Back
                </ThemedText>
              </Pressable>
            </View>

            <View style={styles.card}>
              <TextField label="Permit Token (QR Code Text)" value={validateToken} onChangeText={setValidateToken} placeholder="Paste the scanned token here..." multiline />
              <View style={{ marginTop: 12 }}>
                <PrimaryButton title={validating ? "Validating..." : "Validate"} onPress={validateGroupToken} disabled={validating} loading={validating} />
              </View>
            </View>

            {validateResult ? (
              <View style={styles.card}>
                <ThemedText style={styles.cardTitle}>{validateResult.valid ? "Valid" : "Not Valid"}</ThemedText>
                <ThemedText style={styles.cardMeta}>Permit No.: {validateResult.group_permit_number || "—"}</ThemedText>
                <ThemedText style={styles.cardMeta}>License Plate: {validateResult.license_plate}</ThemedText>
                <ThemedText style={styles.cardMeta}>Destination: {validateResult.destination_salesfloor}</ThemedText>
                <ThemedText style={styles.cardMeta}>Valid To: {formatDate(validateResult.valid_to)}</ThemedText>
                <ThemedText style={styles.cardMeta}>Status: {validateResult.status}</ThemedText>
                <View style={{ marginTop: 10 }}>
                  {(validateResult.entries || []).map((e: any) => (
                    <View key={String(e.id)} style={styles.entryRow}>
                      <View style={{ flex: 1 }}>
                        <ThemedText style={styles.entryTitle}>{e.grower_name || e.grower_tbz_id || "Grower"}</ThemedText>
                        <ThemedText style={styles.entryMeta}>{e.total_bales} bales • {String(e.total_weight_kg)} kg</ThemedText>
                      </View>
                    </View>
                  ))}
                </View>
              </View>
            ) : null}
          </ScrollView>
        </KeyboardAvoidingView>

        <Modal visible={scannerOpen} animationType="slide" onRequestClose={() => setScannerOpen(false)}>
          <View style={{ flex: 1, backgroundColor: "#000" }}>
            <CameraView
              style={{ flex: 1 }}
              onBarcodeScanned={onBarcodeScanned}
              barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
            />
            <View style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: 16, gap: 10 }}>
              <Pressable
                onPress={() => setScannerOpen(false)}
                style={({ pressed }) => [styles.scanCloseBtn, pressed && styles.scanCloseBtnPressed]}
              >
                <MaterialIcons name="close" size={20} color="#FFFFFF" />
                <ThemedText style={styles.scanCloseText}>Close</ThemedText>
              </Pressable>
            </View>
          </View>
        </Modal>
      </ThemedView>
    );
  }

  if (mode === "approve" && selectedGp) {
    return (
      <ThemedView style={styles.container}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {renderHeader("Review Group Permit")}
          <View style={styles.card}>
            <ThemedText style={styles.cardTitle}>{selectedGp.group_permit_number || "Pending"}</ThemedText>
            <ThemedText style={styles.cardMeta}>{selectedGp.license_plate} → {selectedGp.destination_salesfloor}</ThemedText>
            <ThemedText style={styles.cardMeta}>Entries: {(selectedGp.entries ?? []).length}</ThemedText>
          </View>

          <View style={styles.card}>
            <ThemedText style={styles.cardTitle}>Action</ThemedText>
            <View style={{ flexDirection: "row", gap: 10 }}>
              <Pressable onPress={() => setReviewAction("approve")} style={[styles.pill, reviewAction === "approve" && styles.pillActive]}>
                <ThemedText style={[styles.pillText, reviewAction === "approve" && styles.pillTextActive]}>Approve</ThemedText>
              </Pressable>
              <Pressable onPress={() => setReviewAction("reject")} style={[styles.pill, reviewAction === "reject" && styles.pillActive]}>
                <ThemedText style={[styles.pillText, reviewAction === "reject" && styles.pillTextActive]}>Reject</ThemedText>
              </Pressable>
            </View>

            {reviewAction === "approve" ? (
              <>
                <Pressable onPress={() => setShowValidFromPicker(true)} style={styles.dateBtn}>
                  <ThemedText style={styles.dateBtnText}>Valid From: {toYmd(validFrom)}</ThemedText>
                </Pressable>
                <Pressable onPress={() => setShowValidToPicker(true)} style={styles.dateBtn}>
                  <ThemedText style={styles.dateBtnText}>Valid To: {toYmd(validTo)}</ThemedText>
                </Pressable>
              </>
            ) : (
              <View style={styles.rejectBox}>
                <ThemedText style={styles.rejectLabel}>Reason *</ThemedText>
                <TextInput value={rejectReason} onChangeText={setRejectReason} style={styles.rejectInput} multiline placeholder="Enter reason..." />
              </View>
            )}

            <View style={{ marginTop: 12, gap: 10 }}>
              <PrimaryButton title={approving ? "Submitting..." : "Submit"} onPress={submitApproval} disabled={approving} loading={approving} />
              <PrimaryButton title="Back" onPress={openList} />
            </View>
          </View>
        </ScrollView>

        {showValidFromPicker ? (
          <DateTimePicker
            value={validFrom}
            mode="date"
            onChange={(_, date) => {
              setShowValidFromPicker(false);
              if (date) setValidFrom(date);
            }}
          />
        ) : null}
        {showValidToPicker ? (
          <DateTimePicker
            value={validTo}
            mode="date"
            onChange={(_, date) => {
              setShowValidToPicker(false);
              if (date) setValidTo(date);
            }}
          />
        ) : null}
      </ThemedView>
    );
  }

  if (mode === "detail" && selectedGp) {
    const badge = statusBadgeColors(selectedGp.status);
    return (
      <ThemedView style={styles.container}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {renderHeader("Details")}
          <View style={styles.card}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
              <ThemedText style={styles.cardTitle}>{selectedGp.group_permit_number || selectedGp.status}</ThemedText>
              <View style={[styles.badge, { backgroundColor: badge.bg }]}>
                <ThemedText style={[styles.badgeText, { color: badge.text }]}>{selectedGp.status}</ThemedText>
              </View>
            </View>
            <ThemedText style={styles.cardMeta}>License Plate: {selectedGp.license_plate}</ThemedText>
            <ThemedText style={styles.cardMeta}>Destination: {selectedGp.destination_salesfloor}</ThemedText>
            <ThemedText style={styles.cardMeta}>Valid To: {formatDate(selectedGp.valid_to)}</ThemedText>
            <ThemedText style={styles.cardMeta}>Total: {selectedGp.total_bales} bales • {String(selectedGp.total_weight_kg)} kg</ThemedText>
          </View>

          <View style={styles.card}>
            <ThemedText style={styles.cardTitle}>Growers ({(selectedGp.entries ?? []).length})</ThemedText>
            {(selectedGp.entries ?? []).map((e) => (
              <View key={e.id} style={styles.entryRow}>
                <View style={{ flex: 1 }}>
                  <ThemedText style={styles.entryTitle}>{e.grower_name || e.grower_tbz_id || "Grower"}</ThemedText>
                  <ThemedText style={styles.entryMeta}>{e.total_bales} bales • {String(e.total_weight_kg)} kg</ThemedText>
                </View>
              </View>
            ))}
          </View>

          <View style={{ gap: 10 }}>
            {selectedGp.status === "DRAFT" ? <PrimaryButton title="Edit Entries" onPress={() => openEntries(selectedGp)} /> : null}
            {selectedGp.status === "PENDING" && canApprovePermits ? <PrimaryButton title="Review & Approve" onPress={() => openApprove(selectedGp)} /> : null}
            <PrimaryButton title="Back to List" onPress={openList} />
          </View>
        </ScrollView>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {renderHeader(listSubtitleText)}
        {showDashboard ? renderActionsRow() : null}

        {showDashboard ? (
          <View style={styles.kpiGrid}>
            <Pressable onPress={openCreate} style={[styles.kpiCard, styles.kpiCardAccent]}>
              <View style={styles.kpiTop}>
                <ThemedText style={[styles.kpiLabelAccent, styles.kpiLabelText]} numberOfLines={1} ellipsizeMode="tail">
                  New Group Permit
                </ThemedText>
                <MaterialIcons name="add-circle-outline" size={18} color="#0B6B3A" />
              </View>
              <View style={styles.kpiValueRow}>
                <MaterialIcons name="groups" size={26} color="#0B6B3A" />
              </View>
              <ThemedText style={styles.kpiSub}>create a grouped permit</ThemedText>
            </Pressable>

            <Pressable onPress={() => router.push({ pathname: "/permit-group-status" as any, params: { status: "DRAFT" } } as any)} style={styles.kpiCard}>
              <View style={styles.kpiTop}>
                <ThemedText style={[styles.kpiLabel, styles.kpiLabelText]} numberOfLines={1} ellipsizeMode="tail">
                  Draft
                </ThemedText>
                <MaterialIcons name="edit-square" size={18} color="#6B7280" />
              </View>
              <ThemedText style={styles.kpiValueSecondary}>{kpiDraft ?? 0}</ThemedText>
              <ThemedText style={styles.kpiSub}>being built</ThemedText>
            </Pressable>

            <Pressable onPress={() => router.push({ pathname: "/permit-group-status" as any, params: { status: "PENDING" } } as any)} style={styles.kpiCard}>
              <View style={styles.kpiTop}>
                <ThemedText style={[styles.kpiLabel, styles.kpiLabelText]} numberOfLines={1} ellipsizeMode="tail">
                  Pending Review
                </ThemedText>
                <MaterialIcons name="hourglass-bottom" size={18} color="#6B7280" />
              </View>
              <ThemedText style={styles.kpiValueWarning}>{kpiPending ?? 0}</ThemedText>
              <ThemedText style={styles.kpiSub}>awaiting inspector</ThemedText>
            </Pressable>

            <Pressable onPress={() => router.push({ pathname: "/permit-group-status" as any, params: { status: "APPROVED" } } as any)} style={styles.kpiCard}>
              <View style={styles.kpiTop}>
                <ThemedText style={[styles.kpiLabel, styles.kpiLabelText]} numberOfLines={1} ellipsizeMode="tail">
                  Active
                </ThemedText>
                <MaterialIcons name="check-circle-outline" size={18} color="#6B7280" />
              </View>
              <ThemedText style={styles.kpiValueSuccess}>{kpiActive ?? 0}</ThemedText>
              <ThemedText style={styles.kpiSub}>approved and valid</ThemedText>
            </Pressable>
          </View>
        ) : null}

        {showStatusPills ? (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.pillsRow}>
            {statusPills.map((p) => {
              const active = p.value === selectedStatus;
              return (
                <Pressable key={p.value || "ALL"} onPress={() => onChangeStatus(p.value)} style={[styles.pill, active && styles.pillActive]}>
                  <ThemedText style={[styles.pillText, active && styles.pillTextActive]}>{p.label}</ThemedText>
                </Pressable>
              );
            })}
          </ScrollView>
        ) : null}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#FFFFFF" },
  content: { padding: 20, paddingBottom: 40, gap: 14 },
  header: { flexDirection: "row", alignItems: "center", gap: 12 },
  title: { fontSize: 22, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  subtitle: { marginTop: 4, fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, color: "#6B7280" },
  closeBtn: { padding: 8 },
  card: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 16, padding: 14, backgroundColor: "#FFFFFF", gap: 10 },
  cardTitle: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  cardMeta: { fontSize: 12, color: "#6B7280" },
  actionsRow: { flexDirection: "row", gap: 10 },
  actionBtn: { flex: 1, minHeight: 46, borderRadius: 12, paddingVertical: 12, paddingHorizontal: 12, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8 },
  actionBtnOutline: { backgroundColor: "#FFFFFF", borderWidth: 1 },
  actionBtnOutlinePressed: { backgroundColor: "#E8F3EE" },
  actionBtnPrimary: { borderWidth: 1, borderColor: "transparent" },
  actionBtnPrimaryPressed: { opacity: Platform.OS === "ios" ? 0.85 : 1 },
  actionBtnTextOutline: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 13, flexShrink: 1 },
  actionBtnTextPrimary: { color: "#FFFFFF", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 13, flexShrink: 1 },
  kpiGrid: { flexDirection: "row", flexWrap: "wrap", gap: 12, justifyContent: "space-between" },
  kpiCard: { width: "48%", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 16, padding: 14, backgroundColor: "#FFFFFF", overflow: "hidden" },
  kpiCardAccent: { backgroundColor: "#E8F3EE", borderColor: "#B7E0CB" },
  kpiTop: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 10 },
  kpiLabelText: { flex: 1, flexShrink: 1, marginRight: 8 },
  kpiLabel: { fontSize: 12, color: "#111827", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  kpiLabelAccent: { fontSize: 12, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  kpiValueRow: { marginTop: 14, alignItems: "flex-start" },
  kpiValueSecondary: { marginTop: 10, fontSize: 22, color: "#6B7280", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  kpiValueWarning: { marginTop: 10, fontSize: 22, color: "#D97706", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  kpiValueSuccess: { marginTop: 10, fontSize: 22, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  kpiSub: { marginTop: 6, fontSize: 12, color: "#6B7280" },
  pillsRow: { gap: 8, paddingVertical: 2 },
  pill: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 999, backgroundColor: "#F3F4F6" },
  pillActive: { backgroundColor: "#111827" },
  pillText: { fontSize: 12, color: "#111827" },
  pillTextActive: { color: "#FFFFFF", fontWeight: FontWeight.bold },
  empty: { marginTop: 18, alignItems: "center", paddingVertical: 28, opacity: 0.85 },
  groupCard: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 16, padding: 14, backgroundColor: "#FFFFFF", gap: 6 },
  groupCardPressed: { backgroundColor: "#F9FAFB" },
  groupTitle: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827", flex: 1 },
  groupMeta: { fontSize: 12, color: "#6B7280" },
  badge: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 999 },
  badgeText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  inlineActions: { marginTop: 8, flexDirection: "row", flexWrap: "wrap", gap: 8, justifyContent: "flex-start" },
  cardActionBtn: { flexDirection: "row", alignItems: "center", gap: 8, paddingHorizontal: 12, paddingVertical: 10, borderRadius: 12, borderWidth: 1 },
  cardActionBtnPressed: { opacity: Platform.OS === "ios" ? 0.85 : 1 },
  cardActionBtnInfo: { backgroundColor: "#EFF6FF", borderColor: "#BFDBFE" },
  cardActionBtnNeutral: { backgroundColor: "#F9FAFB", borderColor: "#E5E7EB" },
  cardActionBtnSuccess: { backgroundColor: "#E8F3EE", borderColor: "#B7E0CB" },
  cardActionText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, flexShrink: 1 },
  cardActionTextInfo: { color: "#2563EB" },
  cardActionTextNeutral: { color: "#111827" },
  cardActionTextSuccess: { color: "#0B6B3A" },
  searchBtn: { height: 56, width: 56, borderRadius: 16, alignItems: "center", justifyContent: "center" },
  searchBtnPressed: { opacity: Platform.OS === "ios" ? 0.85 : 1 },
  resultsWrap: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, overflow: "hidden", marginTop: 10 },
  resultRow: { paddingHorizontal: 12, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: "#F3F4F6" },
  resultTitle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: "#111827" },
  resultMeta: { marginTop: 2, fontSize: 12, color: "#6B7280" },
  entryRow: { flexDirection: "row", alignItems: "center", gap: 10, paddingVertical: 8 },
  entryTitle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  entryMeta: { marginTop: 2, fontSize: 12, color: "#6B7280" },
  removeBtn: { width: 40, height: 40, borderRadius: 10, backgroundColor: "#FEF2F2", alignItems: "center", justifyContent: "center" },
  scanCloseBtn: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8, paddingVertical: 12, borderRadius: 12, backgroundColor: "rgba(0,0,0,0.6)" },
  scanCloseBtnPressed: { backgroundColor: "rgba(0,0,0,0.75)" },
  scanCloseText: { color: "#FFFFFF", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  dateBtn: { marginTop: 10, borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, paddingVertical: 12, paddingHorizontal: 12 },
  dateBtnText: { color: "#111827" },
  rejectBox: { marginTop: 12 },
  rejectLabel: { fontSize: 12, color: "#111827", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, marginBottom: 6 },
  rejectInput: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, minHeight: 80, textAlignVertical: "top" },
});
