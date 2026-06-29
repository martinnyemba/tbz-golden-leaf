import * as Location from "expo-location";
import { Link, useLocalSearchParams, useRouter } from "expo-router";
import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import {
    apiFetchJson,
    getDeviceId,
    type GrowerProfile,
    type TobaccoStage,
} from "@/lib/inspection-storage";

function normalizeId(raw: string) {
  return raw.trim().replace(/\s+/g, "").toLowerCase();
}

type GrowerApi = {
  id?: string;
  tbz_id?: string;
  display_name?: string;
  first_name?: string;
  middle_name?: string;
  last_name?: string;
  nrc_number?: string;
  sponsor?: string;
  province?: string;
  district?: string;
  hectarage?: number;
};

type ApiErrorEnvelope = {
  success?: boolean;
  error?: { message?: string };
};

export default function InspectionLookupScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const params = useLocalSearchParams<{
    stage?: string;
    inspectorName?: string;
  }>();

  const paramStage = (params.stage ?? "") as TobaccoStage | "";
  const paramInspector = params.inspectorName ?? "";
  const [stage, setStage] = useState<TobaccoStage | "">(paramStage);
  const [inspectorName, setInspectorName] = useState(paramInspector);

  const [query, setQuery] = useState("");
  const [grower, setGrower] = useState<GrowerProfile | null>(null);
  const [retrieving, setRetrieving] = useState(false);

  const [gps, setGps] = useState("");
  const [deviceId, setDeviceId] = useState("");
  const [locationError, setLocationError] = useState("");

  useEffect(() => {
    (async () => {
      const id = await getDeviceId();
      setDeviceId(id);
    })();
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== "granted") {
          setLocationError("Location permission denied.");
          return;
        }
        setLocationError("");
        try {
          const pos = await Location.getCurrentPositionAsync({});
          setGps(`${pos.coords.latitude.toFixed(6)}, ${pos.coords.longitude.toFixed(6)}`);
        } catch {
          const last = await Location.getLastKnownPositionAsync();
          if (last?.coords) {
            setGps(`${last.coords.latitude.toFixed(6)}, ${last.coords.longitude.toFixed(6)}`);
          } else {
            setLocationError("Current location is unavailable. Make sure that location services are enabled.");
          }
        }
      } catch {
        setLocationError("Current location is unavailable. Make sure that location services are enabled.");
      }
    })();
  }, []);

  const canStart = useMemo(() => {
    return (
      Boolean(stage) &&
      Boolean(grower) &&
      Boolean(gps.trim()) &&
      Boolean(deviceId) &&
      Boolean(inspectorName.trim())
    );
  }, [deviceId, gps, grower, inspectorName, stage]);

  async function retrieveGrower() {
    if (!query.trim()) {
      Alert.alert("Missing input", "Enter Grower ID or NRC Number.");
      return;
    }

    setRetrieving(true);
    try {
      const resp = await apiFetchJson(`/api/v1/growers/growers/?search=${encodeURIComponent(query.trim())}&page_size=20`, {
        method: "GET",
      });
      if (!resp.ok) {
        if (resp.status === 0) {
          router.push({
            pathname: "/portal-login",
            params: {
              returnTo: `/inspection/lookup?stage=${encodeURIComponent(stage)}&inspectorName=${encodeURIComponent(inspectorName)}`,
            },
          } as any);
        }
        return;
      }

      const payloadUnknown = JSON.parse(resp.body) as unknown;
      const errEnv = payloadUnknown as ApiErrorEnvelope;
      if (errEnv?.success === false) {
        Alert.alert("Retrieve failed", errEnv.error?.message ?? "Request failed.");
        return;
      }

      const payload = payloadUnknown as { results?: GrowerApi[] } | GrowerApi[];
      const list = Array.isArray(payload) ? payload : payload.results ?? [];
      const wanted = normalizeId(query);

      const found =
        list.find((g) => normalizeId(g.nrc_number ?? "") === wanted) ??
        list.find((g) => normalizeId(g.tbz_id ?? "") === wanted) ??
        list[0] ??
        null;

      if (!found) {
        setGrower(null);
        Alert.alert("Grower not found", "Grower must exist in the system.");
        return;
      }

      const name =
        found.display_name ??
        [found.first_name, found.middle_name, found.last_name]
          .filter(Boolean)
          .join(" ")
          .trim() ??
        "";

      const profile: GrowerProfile = {
        portalGrowerId: found.id ?? "",
        growerId: found.tbz_id ?? found.id ?? query.trim(),
        nrc: found.nrc_number ?? "",
        name,
        sponsor: found.sponsor ?? "",
        province: found.province ?? "",
        district: found.district ?? "",
        hectarage: typeof found.hectarage === "number" ? found.hectarage : 0,
      };

      setGrower(profile);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Network request failed";
      Alert.alert("Retrieve failed", msg);
    } finally {
      setRetrieving(false);
    }
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.section}>
          <SelectField
            label="Select Tobacco Stage"
            value={stage}
            onChange={(v) => setStage(v as TobaccoStage)}
            options={["Nursery", "Field", "Curing"]}
            required
          />
          <TextField
            label="Inspector Name"
            value={inspectorName}
            onChangeText={setInspectorName}
            required
          />
        </View>

        <View style={styles.section}>
          <TextField
            label="Grower ID or NRC Number"
            value={query}
            onChangeText={setQuery}
            placeholder="e.g., GROWER-0001 or 123456/10/1"
            required
          />
          <PrimaryButton
            title={retrieving ? "Retrieving..." : "Retrieve Grower"}
            onPress={retrieveGrower}
            disabled={retrieving}
          />
          {retrieving ? <LeafLoader size={22} /> : null}
        </View>

        <View
          style={[
            styles.card,
            {
              borderColor: Colors[theme].border,
              backgroundColor: Colors[theme].surface,
            },
          ]}
        >
          <ThemedText type="defaultSemiBold">Grower Profile</ThemedText>
          {grower ? (
            <>
              <ThemedText>Name: {grower.name}</ThemedText>
              <ThemedText>Grower ID: {grower.growerId}</ThemedText>
              <ThemedText>NRC: {grower.nrc}</ThemedText>
              <ThemedText>Sponsor: {grower.sponsor}</ThemedText>
              <ThemedText>
                Location: {grower.province}, {grower.district}
              </ThemedText>
              <ThemedText>Hectarage: {grower.hectarage}</ThemedText>
            </>
          ) : (
            <ThemedText>No grower loaded.</ThemedText>
          )}
        </View>

        <View
          style={[
            styles.card,
            {
              borderColor: Colors[theme].border,
              backgroundColor: Colors[theme].surface,
            },
          ]}
        >
          <ThemedText type="defaultSemiBold">Audit Trail</ThemedText>
          <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          <ThemedText>Device ID: {deviceId || "—"}</ThemedText>
          <ThemedText>GPS: {gps || "—"}</ThemedText>
          {locationError ? (
            <ThemedText style={{ color: Colors[theme].accent }}>
              {locationError}
            </ThemedText>
          ) : null}
          <Pressable
            style={[styles.smallButton, { borderColor: Colors[theme].border }]}
            onPress={async () => {
              try {
                const { status } = await Location.requestForegroundPermissionsAsync();
                if (status !== "granted") {
                  setLocationError("Location permission denied.");
                  return;
                }
                setLocationError("");
                try {
                  const pos = await Location.getCurrentPositionAsync({});
                  setGps(`${pos.coords.latitude.toFixed(6)}, ${pos.coords.longitude.toFixed(6)}`);
                } catch {
                  const last = await Location.getLastKnownPositionAsync();
                  if (last?.coords) {
                    setGps(`${last.coords.latitude.toFixed(6)}, ${last.coords.longitude.toFixed(6)}`);
                  } else {
                    setLocationError("Current location is unavailable. Make sure that location services are enabled.");
                  }
                }
              } catch {
                setLocationError("Current location is unavailable. Make sure that location services are enabled.");
              }
            }}
          >
            <ThemedText type="link">Refresh GPS</ThemedText>
          </Pressable>
        </View>

        <PrimaryButton
          title="Start Inspection"
          disabled={!canStart}
          onPress={() => {
            if (!stage) {
              Alert.alert(
                "Missing stage",
                "Go back and select an inspection stage.",
              );
              return;
            }
            if (!grower) {
              Alert.alert("Missing grower", "Retrieve a grower profile first.");
              return;
            }
            if (!gps.trim()) {
              Alert.alert("Missing GPS", "GPS must be auto-captured.");
              return;
            }
            if (!deviceId) {
              Alert.alert("Missing device ID", "Device ID is required.");
              return;
            }
            const pathname =
              stage === "Nursery"
                ? "/inspection/nursery"
                : stage === "Field"
                  ? "/inspection/field"
                  : "/inspection/curing";

            router.push({
              pathname: pathname as any,
              params: {
                inspectorName,
                stage,
                grower: JSON.stringify(grower),
                gps,
                deviceId,
              },
            });
          }}
        />

        <Link href={"/inspection" as any} asChild>
          <Pressable style={styles.linkButton}>
            <ThemedText type="link">Back to Inspection</ThemedText>
          </Pressable>
        </Link>
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
  section: {
    gap: 10,
  },
  card: {
    borderWidth: 1,
    borderRadius: 14,
    padding: 14,
    gap: 6,
    overflow: "hidden",
  },
  smallButton: {
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderWidth: 1,
    borderRadius: 12,
    alignSelf: "flex-start",
  },
  linkButton: {
    paddingVertical: 6,
  },
});
