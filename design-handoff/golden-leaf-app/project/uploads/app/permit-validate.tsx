import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from "expo-camera";
import { Stack, useFocusEffect, useRouter } from "expo-router";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { SelectField, TextField } from "@/components/ui/form-controls";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useNetworkStatus } from "@/lib/network-state";
import { apiErrorUi, apiFetchJson, extractErrorMessage, lookupPermitOffline } from "@/lib/inspection-storage";

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

export default function PermitValidateScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [permission, requestPermission] = useCameraPermissions();
  const [isScannerOpen, setIsScannerOpen] = useState(false);
  const [scanned, setScanned] = useState(false);

  const [salesfloors, setSalesfloors] = useState<any[]>([]);
  const [loadingSalesfloors, setLoadingSalesfloors] = useState(false);
  const [salesfloorFetchStatus, setSalesfloorFetchStatus] = useState<number | null>(null);
  const [salesfloorFetchError, setSalesfloorFetchError] = useState<string>("");

  const [permitToken, setPermitToken] = useState("");
  const [salesfloorId, setSalesfloorId] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");
  const [permitStatusText, setPermitStatusText] = useState("");
  const [permitFromCache, setPermitFromCache] = useState(false);

  const [validatedGrower, setValidatedGrower] = useState<any>(null);
  const [validatedPermit, setValidatedPermit] = useState<any>(null);
  const [isValidating, setIsValidating] = useState(false);

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

  useFocusEffect(
    useCallback(() => {
      async function fetchLookups() {
        setLoadingSalesfloors(true);
        setSalesfloorFetchError("");
        setSalesfloorFetchStatus(null);
        try {
          const sfs = await fetchAllEntities("SALES_FLOOR");
          setSalesfloors(sfs.filter((sf) => sf?.is_active !== false));
        } catch (e) {
          const msg = e instanceof Error ? e.message : "Failed to load sales floors";
          setSalesfloorFetchError(msg);
          if (!isOffline) Alert.alert("Failed to load sales floors", msg);
          setSalesfloors([]);
        } finally {
          setLoadingSalesfloors(false);
        }
      }

      void fetchLookups();
    }, [isOffline]),
  );

  const sfOptions = useMemo(
    () =>
      salesfloors
        .map((sf) => ({ label: salesfloorLabel(sf), value: String(sf?.id ?? "") }))
        .filter((o) => Boolean(o.label) && Boolean(o.value)),
    [salesfloorLabel, salesfloors],
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
      setSalesfloorId(savedSalesfloorId);
      setProvince(String(payload.salesfloor?.province ?? payload.grower?.province ?? ""));
      setDistrict(String(payload.salesfloor?.district ?? payload.grower?.district ?? ""));
      setPermitStatusText("Permit previously validated.");
      setPermitFromCache(Boolean(payload.fromCache));
    } catch {}
  }, [safeJson]);

  useEffect(() => {
    void restoreValidatedPermit();
  }, [restoreValidatedPermit]);

  const clearValidatedPermit = useCallback(async () => {
    await AsyncStorage.removeItem(VALIDATED_PERMIT_KEY);
    setPermitToken("");
    setSalesfloorId("");
    setProvince("");
    setDistrict("");
    setValidatedGrower(null);
    setValidatedPermit(null);
    setPermitStatusText("");
    setPermitFromCache(false);
  }, []);

  const openScanner = async () => {
    if (!permission?.granted) {
      const { granted } = await requestPermission();
      if (!granted) {
        Alert.alert("Camera Permission Required", "Please allow camera access to scan barcodes.");
        return;
      }
    }
    setScanned(false);
    setIsScannerOpen(true);
  };

  const handleBarCodeScanned = ({ data }: BarcodeScanningResult) => {
    setScanned(true);
    setIsScannerOpen(false);
    setPermitToken(String(data ?? ""));
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
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/permit-validate" } } as any) },
            { text: "OK" },
          ]);
        } else if (ui.kind === "network_error") {
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
      setPermitStatusText("Permit validated.");
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
    } catch (e: any) {
      const raw = e instanceof Error ? e.message : String(e ?? "");
      Alert.alert("Error", extractErrorMessage(raw) || "An unexpected error occurred.");
      setValidatedGrower(null);
      setValidatedPermit(null);
    } finally {
      setIsValidating(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ headerShown: false }} />
      <Modal visible={isScannerOpen} animationType="slide" onRequestClose={() => setIsScannerOpen(false)}>
        <View style={styles.scannerWrapper}>
          <View style={styles.scannerHeader}>
            <ThemedText style={{ color: "#FFFFFF", fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }}>
              Scan Permit Token
            </ThemedText>
            <Pressable onPress={() => setIsScannerOpen(false)}>
              <MaterialIcons name="close" size={28} color="#FFFFFF" />
            </Pressable>
          </View>
          <CameraView
            style={StyleSheet.absoluteFillObject}
            facing="back"
            barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
            onBarcodeScanned={scanned ? undefined : handleBarCodeScanned}
          />
          <View style={styles.scannerOverlay}>
            <View style={styles.scannerCutout} />
            <ThemedText style={{ color: "#FFFFFF", marginTop: 32, fontSize: 13, textAlign: "center" }}>
              Align the barcode within the frame to scan.
            </ThemedText>
          </View>
        </View>
      </Modal>

      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : undefined}>
        <View style={[styles.headerRow, { paddingTop: Math.max(12, insets.top + 10) }]}>
          <Pressable onPress={() => router.back()} style={{ padding: 8, marginLeft: -8, marginRight: 8 }}>
            <MaterialIcons name="arrow-back" size={26} color="#FFFFFF" />
          </Pressable>
          <View style={{ flex: 1 }}>
            <ThemedText type="subtitle" style={{ fontSize: 16, color: "#FFFFFF" }}>Validate Permit</ThemedText>
            <ThemedText style={{ fontSize: 12, color: "#D1FAE5" }}>Verify Permit QR Token</ThemedText>
          </View>
        </View>

        {isOffline && (
          <View style={styles.offlineBanner}>
            <MaterialIcons name="wifi-off" size={15} color="#FFFFFF" />
            <ThemedText style={styles.offlineBannerText}>
              You are offline — lookup uses local cache when available
            </ThemedText>
          </View>
        )}

        {permitFromCache && isPermitValidated && (
          <View style={styles.cacheWarning}>
            <MaterialIcons name="warning" size={15} color="#92400E" />
            <ThemedText style={styles.cacheWarningText}>Permit data is from local cache and may be stale</ThemedText>
          </View>
        )}

        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <View style={styles.wizardCard}>
            <View style={styles.wizardCardTitleBar}>
              <MaterialIcons name="qr-code-scanner" size={20} color="#0B6B3A" style={{ marginRight: 8 }} />
              <ThemedText type="defaultSemiBold">Step 1 - Validate Permit</ThemedText>
            </View>
            <ThemedText style={{ fontSize: 12, color: "#4B5563", marginBottom: 16 }}>
              {isOffline
                ? "Offline: enter a permit number to look up from local cache."
                : "Validate permit and sales floor."}
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

            <View style={{ flexDirection: "row", alignItems: "flex-end", gap: 10, marginBottom: 12 }}>
              <View style={{ flex: 1 }}>
                <TextField
                  label={isOffline ? "Permit Number * (offline lookup)" : "Permit QR Token *"}
                  value={permitToken}
                  onChangeText={setPermitToken}
                  placeholder="Scan or manually input…"
                  editable={!isPermitValidated}
                />
              </View>
              <Pressable style={styles.scanBtnMini} onPress={openScanner} disabled={isPermitValidated}>
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
                <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Remaining Bales:</ThemedText><ThemedText style={[styles.infoValue, { color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>{validatedPermit.remaining_bales}</ThemedText></View>
                <View style={styles.infoRow}><ThemedText style={styles.infoLabel}>Remaining Weight:</ThemedText><ThemedText style={[styles.infoValue, { color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold }]}>{validatedPermit.remaining_weight_kg} kg</ThemedText></View>
              </View>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F9FAFB" },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingBottom: 16,
    backgroundColor: "#0B6B3A",
    borderBottomWidth: 0,
  },
  offlineBanner: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#374151",
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  offlineBannerText: { color: "#FFFFFF", fontSize: 12, flex: 1 },
  cacheWarning: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#FEF3C7",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: "#FDE68A",
  },
  cacheWarningText: { color: "#92400E", fontSize: 12, flex: 1 },
  content: { padding: 16, paddingBottom: 60, gap: 16 },
  wizardCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    shadowColor: "#000",
    shadowOpacity: 0.02,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  wizardCardTitleBar: { flexDirection: "row", alignItems: "center", borderBottomWidth: 1, borderBottomColor: "#F3F4F6", paddingBottom: 12, marginBottom: 16 },
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
  permitInfoPanel: { backgroundColor: "#E8F3EE", borderRadius: 8, padding: 12, marginTop: 8, gap: 6 },
  infoRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  infoLabel: { fontSize: 12, color: "#374151" },
  infoValue: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: "#111827" },
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
  scannerWrapper: { flex: 1, backgroundColor: "#000000" },
  scannerHeader: { position: "absolute", top: 50, left: 20, right: 20, flexDirection: "row", justifyContent: "space-between", alignItems: "center", zIndex: 10, backgroundColor: "rgba(0,0,0,0.5)", padding: 12, borderRadius: 12 },
  scannerOverlay: { ...StyleSheet.absoluteFillObject, justifyContent: "center", alignItems: "center", zIndex: 5 },
  scannerCutout: { width: 250, height: 250, borderWidth: 2, borderColor: "#0B6B3A", backgroundColor: "transparent", borderRadius: 12 },
});
