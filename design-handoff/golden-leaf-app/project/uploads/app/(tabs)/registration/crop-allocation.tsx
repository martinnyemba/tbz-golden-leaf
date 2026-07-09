import React, { useState, useEffect, useMemo } from "react";
import { View, ScrollView, StyleSheet, Pressable, Alert } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import * as Location from "expo-location";
import { LeafLoader } from "@/components/LeafLoader";
import { ThemedView } from "@/components/themed-view";
import { ThemedText } from "@/components/themed-text";
import { TextField, SelectField, PrimaryButton } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiFetchJson, ensureGrowerSeasonId, extractErrorMessage } from "@/lib/inspection-storage";
import { readCacheItems, writeCache } from "@/lib/offline-cache";

const TOBACCO_TYPES = ["Flue Cured Tobacco", "Burley", "Dark Fired Tobacco"];

const TOBACCO_TYPE_MAP: Record<string, string> = {
  "Flue Cured Tobacco": "FLUE_CURED",
  "Burley": "BURLEY",
  "Dark Fired Tobacco": "DARK_FIRED"
};
const TOBACCO_TYPE_REVERSE: Record<string, string> = {
  "FLUE_CURED": "Flue Cured Tobacco",
  "BURLEY": "Burley",
  "DARK_FIRED": "Dark Fired Tobacco"
};

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

function CheckRow({ label, checked, onChange }: { label: string; checked: boolean; onChange: (next: boolean) => void }) {
  return (
    <Pressable onPress={() => onChange(!checked)} style={({ pressed }) => [styles.checkRow, { opacity: pressed ? 0.92 : 1 }]}>
      <View style={[styles.checkbox, { backgroundColor: checked ? "#0B6B3A" : "transparent", borderColor: checked ? "#0B6B3A" : "#D1D5DB" }]}>
        {checked ? <MaterialIcons name="check" size={16} color="#FFFFFF" /> : null}
      </View>
      <ThemedText type="defaultSemiBold">{label}</ThemedText>
    </Pressable>
  );
}

export default function CropAllocationScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const params = useLocalSearchParams<{ growerId?: string; cropId?: string }>();
  const growerId = params.growerId;
  const cropId = params.cropId;

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [sponsorsRaw, setSponsorsRaw] = useState<{ id: string; name: string }[]>([]);

  const [tobaccoType, setTobaccoType] = useState("");
  const [sponsorName, setSponsorName] = useState("");
  const [selfSponsored, setSelfSponsored] = useState(false);
  const [hectarage, setHectarage] = useState("");
  const [numberOfBarns, setNumberOfBarns] = useState("");
  const [barnType, setBarnType] = useState("");
  const [stringsPerBarn, setStringsPerBarn] = useState("");
  const [gpsLatitude, setGpsLatitude] = useState("");
  const [gpsLongitude, setGpsLongitude] = useState("");
  const [locationError, setLocationError] = useState("");
  const [barnTypeOptions, setBarnTypeOptions] = useState<RefOption[]>([]);

  const sponsorOptions = useMemo(() => ["", ...sponsorsRaw.map((s) => s.name)], [sponsorsRaw]);
  const barnOptions = useMemo(() => (barnTypeOptions.length > 0 ? barnTypeOptions : FALLBACK_BARN_TYPE_OPTIONS), [barnTypeOptions.length]);

  useEffect(() => {
    (async () => {
      try {
        const cachedBarns = await readCacheItems<any>("ref_barn_types");
        if (cachedBarns.length > 0) {
          const opts = cachedBarns
            .filter((x) => x && (x.code || x.label))
            .map((x) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
          if (opts.length > 0) setBarnTypeOptions(opts);
        }

        const barnsResp = await apiFetchJson("/api/v1/mobile/reference/barn-types/", { method: "GET" });
        if (barnsResp.ok) {
          const payload = JSON.parse(barnsResp.body) as any;
          const rows = Array.isArray(payload) ? payload : payload?.results ?? [];
          const opts = rows
            .filter((x: any) => x && (x.code || x.label))
            .map((x: any) => ({ label: String(x.label ?? x.code), value: String(x.code ?? x.label) }));
          if (opts.length > 0) {
            setBarnTypeOptions(opts);
            void writeCache("ref_barn_types", rows);
          }
        }

        const sponsorsResp = await apiFetchJson("/api/v1/growers/sponsors/?limit=1000", { method: "GET" });
        if (sponsorsResp.ok) {
          const body = JSON.parse(sponsorsResp.body);
          setSponsorsRaw(body.results || []);
        }

        if (cropId) {
          const cropResp = await apiFetchJson(`/api/v1/growers/crop-allocations/${cropId}/`, { method: "GET" });
          if (cropResp.ok) {
            const crop = JSON.parse(cropResp.body);
            setTobaccoType(TOBACCO_TYPE_REVERSE[crop.tobacco_type] || "");
            setBarnType(String(crop.barn_type ?? ""));
            setHectarage(crop.hectarage ? String(crop.hectarage) : "");
            setNumberOfBarns(crop.number_of_barns ? String(crop.number_of_barns) : "");
            setStringsPerBarn(crop.number_of_strings_per_barn ? String(crop.number_of_strings_per_barn) : "");
            setGpsLatitude(crop.gps_latitude || "");
            setGpsLongitude(crop.gps_longitude || "");
            setSelfSponsored(!!crop.is_self_sponsored);
            if (crop.sponsor_name) setSponsorName(crop.sponsor_name);
          }
        }
      } catch (e) {
        console.log("Failed to load initial data", e);
      } finally {
        setLoading(false);
      }
    })();
  }, [cropId]);

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

  const canSubmit = useMemo(() => {
    if (!tobaccoType) return false;
    if (!hectarage.trim()) return false;
    if (!numberOfBarns.trim()) return false;
    if (!barnType) return false;
    if (!stringsPerBarn.trim()) return false;
    if (!selfSponsored && !sponsorName) return false;
    return true;
  }, [tobaccoType, hectarage, numberOfBarns, barnType, stringsPerBarn, selfSponsored, sponsorName]);

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      if (!growerId) throw new Error("Missing Grower ID");

      const matchingSponsor = sponsorsRaw.find(s => s.name === sponsorName);
      const spId = selfSponsored ? null : matchingSponsor?.id;

      const now = new Date();
      const year = now.getFullYear();
      const month = now.getMonth() + 1;
      const seasonStr = month >= 4 ? `${year}/${year + 1}` : `${year - 1}/${year}`;
      const seasonRes = await ensureGrowerSeasonId(growerId, seasonStr);
      if ("error" in seasonRes) throw new Error(`Failed to create working Grower Season: ${seasonRes.error}`);
      const growerSeasonId = seasonRes.id;

      const payload = {
        grower_season: growerSeasonId,
        sponsor: spId,
        is_self_sponsored: selfSponsored,
        tobacco_type: TOBACCO_TYPE_MAP[tobaccoType],
        hectarage: parseFloat(hectarage) || 0,
        number_of_barns: parseInt(numberOfBarns, 10) || 0,
        barn_type: barnType,
        number_of_strings_per_barn: parseInt(stringsPerBarn, 10) || 0,
        gps_latitude: gpsLatitude || null,
        gps_longitude: gpsLongitude || null
      };

      let resp;
      if (cropId) {
        resp = await apiFetchJson(`/api/v1/growers/crop-allocations/${cropId}/`, {
          method: "PATCH",
          body: JSON.stringify({
            sponsor: payload.sponsor,
            is_self_sponsored: payload.is_self_sponsored,
            tobacco_type: payload.tobacco_type,
            hectarage: payload.hectarage,
            number_of_barns: payload.number_of_barns,
            barn_type: payload.barn_type,
            number_of_strings_per_barn: payload.number_of_strings_per_barn,
            gps_latitude: payload.gps_latitude,
            gps_longitude: payload.gps_longitude,
          })
        });
      } else {
        resp = await apiFetchJson("/api/v1/growers/crop-allocations/", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      }

      if (!resp.ok) {
        const errorMsg = extractErrorMessage(resp.body);
        Alert.alert("Failure", `Error updating crop allocation: ${errorMsg}`);
        return;
      }

      Alert.alert("Success", "Crop allocation successfully updated.", [
        { text: "OK", onPress: () => router.back() }
      ]);
    } catch (e: any) {
      Alert.alert("Error", extractErrorMessage(e?.message) || "An unexpected error occurred.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <ThemedView style={styles.centerMode}>
        <LeafLoader size={42} />
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <ThemedText type="defaultSemiBold" style={styles.cardTitle}>
              {cropId ? "Update Crop Allocation" : "Add Crop Allocation"}
            </ThemedText>
          </View>

          <View style={styles.cardBody}>
            <SelectField
              label={"Crop Type"}
              value={tobaccoType}
              onChange={setTobaccoType}
              options={TOBACCO_TYPES}
              required
            />

            <SelectField
              label="Sponsor"
              value={sponsorName}
              onChange={setSponsorName}
              options={sponsorOptions}
            />

            <CheckRow
              label="Self-Sponsored"
              checked={selfSponsored}
              onChange={(next) => {
                setSelfSponsored(next);
                if (next) setSponsorName("");
              }}
            />

            <TextField
              label={"Hectarage"}
              value={hectarage}
              onChangeText={setHectarage}
              keyboardType="decimal-pad"
              required
            />

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
                  label="GPS Latitude"
                  value={gpsLatitude}
                  onChangeText={setGpsLatitude}
                />
              </View>
              <View style={styles.gridCol}>
                <TextField
                  label="GPS Longitude"
                  value={gpsLongitude}
                  onChangeText={setGpsLongitude}
                />
              </View>
            </View>
            {locationError ? <ThemedText style={{ color: "red" }}>{locationError}</ThemedText> : null}
          </View>
        </View>

        <View style={styles.actions}>
          <Pressable
            style={[styles.btn, styles.btnSecondary, { flex: 1, borderColor: Colors[theme].border }]}
            onPress={() => router.back()}
          >
            <ThemedText type="defaultSemiBold">Cancel</ThemedText>
          </Pressable>

          <Pressable
            style={[styles.btn, styles.btnSuccess, { flex: 1, opacity: canSubmit ? 1 : 0.6 }]}
            disabled={submitting}
            onPress={() => {
              if (!canSubmit) {
                const missing: string[] = [];
                if (!tobaccoType) missing.push("Crop Type");
                if (!selfSponsored && !sponsorName) missing.push("Sponsor (or Self-Sponsored)");
                if (!hectarage.trim()) missing.push("Total Hectarage");
                if (!numberOfBarns.trim()) missing.push("Number of Barns");
                if (!barnType) missing.push("Barn Type");
                if (!stringsPerBarn.trim()) missing.push("Strings per Barn");
                
                Alert.alert("Missing fields", `Please complete all required fields before submitting:\n\n${missing.join("\n")}`);
                return;
              }
              handleSubmit();
            }}
          >
            <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">
              {submitting ? "Saving..." : "Save"}
            </ThemedText>
          </Pressable>
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  centerMode: { flex: 1, alignItems: "center", justifyContent: "center" },
  content: { padding: 16, gap: 16 },
  card: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 16, overflow: "hidden" },
  cardHeader: { padding: 14, backgroundColor: "#F9FAFB", borderBottomWidth: 1, borderBottomColor: "#E5E7EB" },
  cardTitle: { fontSize: 16 },
  cardBody: { padding: 14, gap: 12 },
  checkRow: { flexDirection: "row", alignItems: "center", gap: 8, paddingVertical: 4 },
  checkbox: { width: 22, height: 22, borderRadius: 6, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  gridRow: { flexDirection: "row", gap: 10 },
  gridCol: { flex: 1 },
  actions: { flexDirection: "row", gap: 10, justifyContent: "space-between", marginTop: 20, paddingBottom: 20 },
  btn: { paddingHorizontal: 16, paddingVertical: 14, borderRadius: 12, borderWidth: 1, justifyContent: "center", alignItems: "center" },
  btnSecondary: { backgroundColor: "transparent" },
  btnSuccess: { backgroundColor: "#0B6B3A", borderColor: "#0B6B3A" },
});
