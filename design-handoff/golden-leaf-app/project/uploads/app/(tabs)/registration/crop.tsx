import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import * as Location from "expo-location";
import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, View } from "react-native";

import { FullScreenLeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { SelectField, TextField } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiFetchJson, saveGrowerRegistration } from "@/lib/inspection-storage";
import { readCacheItems, writeCache } from "@/lib/offline-cache";

type GrowerTypeCode = "INDIVIDUAL" | "COMPANY";

type GrowerRegistrationDraft = {
  step: 1 | 2;
  growerType: GrowerTypeCode;
  firstName: string;
  middleName: string;
  lastName: string;
  nrcNumber: string;
  sex: string;
  dateOfBirth: string;
  category: string;
  phoneNumber: string;
  email: string;
  address: string;
  townOrVillage: string;
  province: string;
  district: string;
  gpsLatitude: string;
  gpsLongitude: string;
  profilePhotoUri: string;
  idFrontUri: string;
  idBackUri: string;
  auditDateTime: string;
  editingId?: string;
  crop?: any;
};

const DRAFT_KEY = "tbz:growerRegistrationDraft:v1";
const CLEAR_FORM_KEY = "tbz:growerRegistrationClearForm:v1";
const TOBACCO_ZM_GREEN = "#0B6B3A";

const TOBACCO_TYPES = ["Flue Cured Tobacco", "Burley", "Dark Fired Tobacco"];
type RefOption = { label: string; value: string };

const FALLBACK_BARN_TYPE_OPTIONS: RefOption[] = [
  { label: "Flue Cured Barn", value: "FLUE_CURED" },
  { label: "Bulk Cure Barn", value: "BULK_CURE" },
  { label: "Air Cured Barn", value: "AIR_CURED" },
  { label: "Fire Cured Barn", value: "FIRE_CURED" },
  { label: "Conventional", value: "CONVENTIONAL" },
  { label: "Traditional", value: "TRADITIONAL" },
  { label: "Rocket", value: "ROCKET" },
  { label: "Chongololo", value: "CHONGOLOLO" },
  { label: "Matope", value: "MATOPE" },
  { label: "Kamanga", value: "KAMANGA" },
  { label: "Tunnel", value: "TUNNEL" },
  { label: "Live Barn", value: "LIVE_BARN" },
];

function WizardStepper({ step }: { step: 1 | 2 }) {
  return (
    <View style={styles.stepper}>
      <View style={[styles.stepItem, styles.stepDone]}>
        <View style={[styles.stepIndex, styles.stepIndexDone]}>
          <MaterialIcons name="check" size={16} color="#FFFFFF" />
        </View>
        <ThemedText type="defaultSemiBold">Personal Info</ThemedText>
      </View>

      <View style={[styles.stepItem, styles.stepActive]}>
        <View style={[styles.stepIndex, styles.stepIndexActive]}>
          <ThemedText style={styles.stepIndexText} lightColor="#FFFFFF" darkColor="#FFFFFF">
            2
          </ThemedText>
        </View>
        <ThemedText type="defaultSemiBold">Crop Info</ThemedText>
      </View>
    </View>
  );
}

function ProgressBar() {
  return (
    <View style={styles.progressTrack}>
      <View style={[styles.progressFill, { width: "100%" }]} />
    </View>
  );
}

function InfoBanner({ left, right }: { left: string; right: string }) {
  return (
    <View style={styles.infoBanner}>
      <MaterialIcons name="verified-user" size={18} color={TOBACCO_ZM_GREEN} />
      <ThemedText>
        <ThemedText type="defaultSemiBold">{left}</ThemedText>
        {" | "}
        <ThemedText type="defaultSemiBold">{right}</ThemedText>
      </ThemedText>
    </View>
  );
}

function Chip({ label }: { label: string }) {
  return (
    <ThemedText style={styles.chipText} lightColor="#0F3D25" darkColor="#0F3D25">
      {label}
    </ThemedText>
  );
}

function CheckRow({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (next: boolean) => void;
}) {
  return (
    <Pressable
      onPress={() => onChange(!checked)}
      style={({ pressed }) => [
        styles.checkRow,
        { opacity: pressed ? 0.92 : 1 },
      ]}
    >
      <View
        style={[
          styles.checkbox,
          { backgroundColor: checked ? TOBACCO_ZM_GREEN : "transparent", borderColor: checked ? TOBACCO_ZM_GREEN : "#D1D5DB" },
        ]}
      >
        {checked ? <MaterialIcons name="check" size={16} color="#FFFFFF" /> : null}
      </View>
      <ThemedText type="defaultSemiBold">{label}</ThemedText>
    </Pressable>
  );
}

export default function CropInfoScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [draft, setDraft] = useState<GrowerRegistrationDraft | null>(null);
  const [draftLoading, setDraftLoading] = useState(true);
  const [draftError, setDraftError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [sponsorOptions, setSponsorOptions] = useState<string[]>([]);
  const [barnTypeOptions, setBarnTypeOptions] = useState<RefOption[]>([]);

  const [tobaccoType, setTobaccoType] = useState("");
  const [sponsor, setSponsor] = useState("");
  const [selfSponsored, setSelfSponsored] = useState(false);
  const [hectarage, setHectarage] = useState("");
  const [numberOfBarns, setNumberOfBarns] = useState("");
  const [barnType, setBarnType] = useState("");
  const [stringsPerBarn, setStringsPerBarn] = useState("");
  const [gpsLatitude, setGpsLatitude] = useState("");
  const [gpsLongitude, setGpsLongitude] = useState("");
  const [locationError, setLocationError] = useState("");

  useFocusEffect(
    useCallback(() => {
      setReloadKey((v) => v + 1);
    }, []),
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setDraftLoading(true);
      setDraftError("");

      try {
        let raw: string | null = null;
        for (let attempt = 0; attempt < 7; attempt += 1) {
          raw = await AsyncStorage.getItem(DRAFT_KEY);
          if (raw) break;
          await new Promise((r) => setTimeout(r, 150));
        }

        if (!raw) {
          if (!cancelled) {
            setDraft(null);
            setDraftError("Registration draft not found. Please go back and try again.");
            setDraftLoading(false);
          }
          return;
        }

        try {
          const parsed = JSON.parse(raw) as GrowerRegistrationDraft;
          if (cancelled) return;
          setDraft(parsed);
          setGpsLatitude(parsed.gpsLatitude ?? "");
          setGpsLongitude(parsed.gpsLongitude ?? "");
          
          if (parsed.crop) {
            if (parsed.crop.tobaccoType) setTobaccoType(parsed.crop.tobaccoType);
            if (parsed.crop.sponsorName) setSponsor(parsed.crop.sponsorName);
            if (parsed.crop.selfSponsored !== undefined) setSelfSponsored(parsed.crop.selfSponsored);
            if (parsed.crop.hectarage) setHectarage(String(parsed.crop.hectarage));
            if (parsed.crop.numberOfBarns) setNumberOfBarns(String(parsed.crop.numberOfBarns));
            if (parsed.crop.barnType) setBarnType(parsed.crop.barnType);
            if (parsed.crop.stringsPerBarn) setStringsPerBarn(String(parsed.crop.stringsPerBarn));
          }
        } catch {
          if (!cancelled) {
            setDraft(null);
            setDraftError("Registration draft is corrupted. Please start again.");
          }
        } finally {
          if (!cancelled) setDraftLoading(false);
        }
      } catch (e) {
        if (!cancelled) {
          const msg = e instanceof Error ? e.message : "Unable to read registration draft.";
          setDraft(null);
          setDraftError(msg);
          setDraftLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const cached = await readCacheItems<{ name?: string }>("sponsors");
      if (!cancelled && cached.length > 0 && sponsorOptions.length === 0) {
        const names = cached.map((s) => String((s as any)?.name ?? "").trim()).filter(Boolean);
        if (names.length > 0) setSponsorOptions(names);
      }

      const out: string[] = [];
      const rawOut: any[] = [];
      for (let page = 1; page <= 25; page += 1) {
        const resp = await apiFetchJson(`/api/v1/growers/sponsors/?ordering=name&page_size=250&page=${page}`, { method: "GET" });
        if (!resp.ok) break;
        try {
          const payload = JSON.parse(resp.body) as any;
          const rows = Array.isArray(payload) ? payload : payload?.results ?? [];
          rawOut.push(...rows);
          for (const s of rows) {
            const name = String(s?.name ?? "").trim();
            if (name) out.push(name);
          }
          if (!payload?.next || rows.length === 0) break;
        } catch {
          break;
        }
      }
      if (!cancelled && out.length > 0) {
        setSponsorOptions(out);
        if (rawOut.length > 0) void writeCache("sponsors", rawOut);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadKey, sponsorOptions.length]);

  const barnOptions = useMemo(
    () => (barnTypeOptions.length > 0 ? barnTypeOptions : FALLBACK_BARN_TYPE_OPTIONS),
    [barnTypeOptions.length],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const cached = await readCacheItems<any>("ref_barn_types");
      if (!cancelled && cached.length > 0 && barnTypeOptions.length === 0) {
        const opts = cached
          .filter((x) => x && (x.code || x.label))
          .map((x) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
        if (opts.length > 0) setBarnTypeOptions(opts);
      }

      const resp = await apiFetchJson("/api/v1/mobile/reference/barn-types/", { method: "GET" });
      if (resp.ok) {
        try {
          const payload = JSON.parse(resp.body) as any;
          const rows = Array.isArray(payload) ? payload : payload?.results ?? [];
          const opts = rows
            .filter((x: any) => x && (x.code || x.label))
            .map((x: any) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
          if (!cancelled && opts.length > 0) {
            setBarnTypeOptions(opts);
            void writeCache("ref_barn_types", rows);
          }
        } catch {
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [barnTypeOptions.length, reloadKey]);

  useEffect(() => {
    if (!barnType.trim()) return;
    if (barnOptions.some((o) => o.value === barnType)) return;
    const needle = barnType.trim().toLowerCase().replace(/\s+/g, " ");
    const found = barnOptions.find((o) => o.label.trim().toLowerCase().replace(/\s+/g, " ") === needle);
    if (found) setBarnType(found.value);
  }, [barnOptions, barnType]);

  useEffect(() => {
    (async () => {
      if (gpsLatitude.trim() && gpsLongitude.trim()) return;
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== "granted") {
          setLocationError("Location permission denied.");
          return;
        }
        setLocationError("");
        try {
          const pos = await Location.getCurrentPositionAsync({});
          setGpsLatitude(pos.coords.latitude.toFixed(6));
          setGpsLongitude(pos.coords.longitude.toFixed(6));
        } catch {
          const last = await Location.getLastKnownPositionAsync();
          if (last?.coords) {
            setGpsLatitude(last.coords.latitude.toFixed(6));
            setGpsLongitude(last.coords.longitude.toFixed(6));
          } else {
            setLocationError("Current location is unavailable. Make sure that location services are enabled.");
          }
        }
      } catch {
        setLocationError("Current location is unavailable. Make sure that location services are enabled.");
      }
    })();
  }, [gpsLatitude, gpsLongitude]);

  const growerDisplayName = useMemo(() => {
    const first = draft?.firstName ?? "";
    const mid = draft?.middleName ?? "";
    const last = draft?.lastName ?? "";
    return [first, mid, last].filter(Boolean).join(" ").trim();
  }, [draft?.firstName, draft?.middleName, draft?.lastName]);

  const canSubmit = useMemo(() => {
    if (!draft) return false;
    if (!tobaccoType) return false;
    if (!hectarage.trim()) return false;
    if (!numberOfBarns.trim()) return false;
    if (!barnType) return false;
    if (!stringsPerBarn.trim()) return false;
    return true;
  }, [draft, tobaccoType, hectarage, numberOfBarns, barnType, stringsPerBarn]);

  if (draftLoading) {
    return <FullScreenLeafLoader label="Loading…" />;
  }

  if (!draft) {
    return (
      <ThemedView style={styles.container}>
        <ScrollView contentContainerStyle={styles.content}>
          <ThemedText>{draftError || "Unable to load registration draft."}</ThemedText>
          <Pressable
            style={[styles.btn, styles.btnSecondary, { borderColor: Colors[theme].border }]}
            onPress={() => setReloadKey((v) => v + 1)}
          >
            <MaterialIcons name="refresh" size={20} color="#111827" />
            <ThemedText type="defaultSemiBold">Retry</ThemedText>
          </Pressable>
          <Pressable
            style={[styles.btn, styles.btnSecondary, { borderColor: Colors[theme].border }]}
            onPress={() => router.replace("/registration")}
          >
            <MaterialIcons name="chevron-left" size={20} color="#111827" />
            <ThemedText type="defaultSemiBold">Back to Registration</ThemedText>
          </Pressable>
        </ScrollView>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.wizardShell}>
          <WizardStepper step={2} />
          <ProgressBar />

          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <ThemedText type="defaultSemiBold" style={styles.cardTitle}>
                Step 2 — Crop Info
              </ThemedText>
              <ThemedText style={styles.cardSubtitle} lightColor={Colors.light.muted} darkColor={Colors.dark.muted}>
                Yield per hectare is calculated automatically by the system.
              </ThemedText>
            </View>

            <View style={styles.cardBody}>
              <View style={styles.nrcBanner}>
                <MaterialIcons name="badge" size={18} color={TOBACCO_ZM_GREEN} />
                <ThemedText>
                  NRC / ID: <ThemedText type="defaultSemiBold">{draft.nrcNumber}</ThemedText>
                  {growerDisplayName ? (
                    <>
                      {" | "}Grower: <ThemedText type="defaultSemiBold">{growerDisplayName}</ThemedText>
                    </>
                  ) : null}
                </ThemedText>
              </View>

              <SelectField
                label={"Crop Type"}
                value={tobaccoType}
                onChange={setTobaccoType}
                options={TOBACCO_TYPES}
                required
              />

              <SelectField
                label={
                  <>
                    Sponsor{" "}
                    <ThemedText style={styles.inlineMuted} lightColor={Colors.light.muted} darkColor={Colors.dark.muted}>
                      (leave blank if self-sponsored)
                    </ThemedText>
                  </>
                }
                value={sponsor}
                onChange={setSponsor}
                options={sponsorOptions}
              />

              <CheckRow
                label="Self-Sponsored"
                checked={selfSponsored}
                onChange={(next) => {
                  setSelfSponsored(next);
                  if (next) setSponsor("");
                }}
              />

              <TextField
                label={"Hectarage"}
                value={hectarage}
                onChangeText={setHectarage}
                keyboardType="decimal-pad"
                required
              />
              <ThemedText style={styles.formText} lightColor={Colors.light.muted} darkColor={Colors.dark.muted}>
                Supports decimals e.g. 0.5
              </ThemedText>

              <ThemedText type="defaultSemiBold">
                Yield per Ha <Chip label="Auto-calculated" />
              </ThemedText>
              <View style={styles.infoBlock}>
                <MaterialIcons name="calculate" size={18} color={TOBACCO_ZM_GREEN} />
                <ThemedText>
                  <ThemedText type="defaultSemiBold">Rule:</ThemedText>{" "}
                  <ThemedText type="defaultSemiBold">1–9 ha:</ThemedText> 1,500 kg/ha{" "}
                  {" | "}
                  <ThemedText type="defaultSemiBold">10+ ha:</ThemedText> 3,000 kg/ha — system calculates expected yield automatically.
                </ThemedText>
              </View>

              <TextField
                label={"Number of Barns"}
                value={numberOfBarns}
                onChangeText={setNumberOfBarns}
                keyboardType="number-pad"
                required
              />

              <SelectField
                label={"Barn Type"}
                value={barnType}
                onChange={setBarnType}
                options={barnOptions}
                required
              />

              <TextField
                label={"Strings per Barn"}
                value={stringsPerBarn}
                onChangeText={setStringsPerBarn}
                keyboardType="number-pad"
                required
              />

              <View style={styles.gridRow}>
                <View style={styles.gridCol}>
                  <TextField
                    label={
                      <>
                        GPS Latitude <Chip label="Auto" />
                      </>
                    }
                    value={gpsLatitude}
                    onChangeText={setGpsLatitude}
                  />
                </View>
                <View style={styles.gridCol}>
                  <TextField
                    label={
                      <>
                        GPS Longitude <Chip label="Auto" />
                      </>
                    }
                    value={gpsLongitude}
                    onChangeText={setGpsLongitude}
                  />
                </View>
              </View>
              {locationError ? (
                <ThemedText style={{ color: Colors[theme].accent }}>
                  {locationError}
                </ThemedText>
              ) : null}

              <View style={styles.divider} />
              <InfoBanner left="Submitted by: TBZ Inspector" right={`Date: ${draft.auditDateTime}`} />
            </View>
          </View>

          <View style={styles.actions}>
            <Pressable
              style={[styles.btn, styles.btnSecondary]}
              onPress={() => router.replace("/registration")}
            >
              <MaterialIcons name="chevron-left" size={20} color="#111827" />
              <ThemedText type="defaultSemiBold">Back</ThemedText>
            </Pressable>

            <Pressable
              style={[
                styles.btn,
                styles.btnSuccess,
                { opacity: canSubmit ? 1 : 0.6 },
              ]}
              disabled={false}
              onPress={async () => {
                if (!canSubmit) {
                  const missing: string[] = [];
                  if (!tobaccoType) missing.push("Tobacco Type");
                  if (!selfSponsored && !sponsor) missing.push("Sponsor (or Self-Sponsored)");
                  if (!hectarage) missing.push("Total Hectarage");
                  if (!numberOfBarns) missing.push("Number of Barns");
                  if (!barnType) missing.push("Barn Type");
                  if (!stringsPerBarn) missing.push("Strings per Barn");
                  
                  Alert.alert("Missing fields", `Please complete all required fields before submitting:\n\n${missing.join("\n")}`);
                  return;
                }

                Alert.alert(
                  "Saved Locally",
                  "This registration is saved on the device and will be synced to the portal later.",
                  [
                    {
                      text: "OK",
                      onPress: async () => {
                        await saveGrowerRegistration({
                          ...(draft.editingId ? { id: draft.editingId } : {}),
                          grower: {
                            growerType: draft.growerType,
                            firstName: draft.firstName,
                            middleName: draft.middleName,
                            lastName: draft.lastName,
                            nrcNumber: draft.nrcNumber,
                            sex: draft.sex,
                            dateOfBirth: draft.dateOfBirth,
                            category: draft.category,
                            phoneNumber: draft.phoneNumber,
                            email: draft.email,
                            address: draft.address,
                            townOrVillage: draft.townOrVillage,
                            province: draft.province,
                            district: draft.district,
                            gpsLatitude: draft.gpsLatitude,
                            gpsLongitude: draft.gpsLongitude,
                            profilePhotoUri: draft.profilePhotoUri,
                            idFrontUri: draft.idFrontUri,
                            idBackUri: draft.idBackUri,
                          },
                          crop: {
                            tobaccoType,
                            sponsorName: sponsor,
                            selfSponsored,
                            hectarage,
                            numberOfBarns,
                            barnType,
                            stringsPerBarn,
                            gpsLatitude,
                            gpsLongitude,
                          },
                        });
                        await AsyncStorage.removeItem(DRAFT_KEY);
                        await AsyncStorage.setItem(CLEAR_FORM_KEY, "1");
                        router.replace("/registration");
                      },
                    },
                  ],
                );
              }}
            >
              <MaterialIcons name="check-circle" size={18} color="#FFFFFF" />
              <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">
                Submit
              </ThemedText>
            </Pressable>
          </View>
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16 },
  wizardShell: { gap: 12 },
  header: { gap: 6 },
  headerTitleRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  headerTitle: { fontSize: 20 },
  headerSubtitle: { lineHeight: 18 },
  stepper: {
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 10,
    paddingVertical: 8,
  },
  stepItem: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    padding: 10,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#E5E7EB",
  },
  stepActive: { borderColor: TOBACCO_ZM_GREEN, backgroundColor: "#E8F3EE" },
  stepDone: { borderColor: TOBACCO_ZM_GREEN },
  stepIndex: {
    width: 26,
    height: 26,
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#9CA3AF",
  },
  stepIndexActive: { backgroundColor: TOBACCO_ZM_GREEN },
  stepIndexDone: { backgroundColor: TOBACCO_ZM_GREEN },
  stepIndexText: { fontSize: 12 },
  progressTrack: {
    height: 8,
    borderRadius: 999,
    backgroundColor: "#E5E7EB",
    overflow: "hidden",
  },
  progressFill: { height: "100%", backgroundColor: TOBACCO_ZM_GREEN },
  card: {
    borderWidth: 1,
    borderColor: "#E5E7EB",
    borderRadius: 16,
    overflow: "hidden",
  },
  cardHeader: { padding: 14, backgroundColor: "#F9FAFB", gap: 6 },
  cardTitle: { fontSize: 16 },
  cardSubtitle: { fontSize: 12, lineHeight: 16 },
  cardBody: { padding: 14, gap: 12 },
  divider: { height: 1, backgroundColor: "#E5E7EB" },
  infoBanner: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#E8F3EE",
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  nrcBanner: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#E8F3EE",
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  inlineMuted: { fontSize: 12 },
  formText: { fontSize: 12 },
  infoBlock: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 10,
    borderWidth: 1,
    borderColor: "#D1D5DB",
    borderRadius: 14,
    backgroundColor: "#F9FAFB",
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  chipText: {
    fontSize: 11,
    backgroundColor: "#D1FAE5",
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
    marginLeft: 8,
  },
  checkRow: { flexDirection: "row", alignItems: "center", gap: 10, paddingVertical: 6 },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  gridRow: { flexDirection: "row", gap: 12, flexWrap: "wrap" },
  gridCol: { flexGrow: 1, flexBasis: 220, gap: 8 },
  actions: { flexDirection: "row", gap: 12, marginTop: 2 },
  btn: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 14,
    paddingVertical: 14,
    paddingHorizontal: 12,
    borderWidth: 1,
  },
  btnSecondary: { backgroundColor: "transparent", borderColor: "#D1D5DB" },
  btnSuccess: { backgroundColor: TOBACCO_ZM_GREEN, borderColor: TOBACCO_ZM_GREEN },
});
