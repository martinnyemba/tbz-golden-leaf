import { useLocalSearchParams, useRouter } from "expo-router";
import * as Location from "expo-location";
import { useEffect, useMemo, useState } from "react";
import { Alert, ScrollView, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import {
    PrimaryButton,
    SelectField,
    TextField,
} from "@/components/ui/form-controls";
import {
    saveInspectionReport,
    getDeviceId,
    type GrowerProfile,
    type InspectionReport,
    type NurseryInspection,
} from "@/lib/inspection-storage";

const GERMINATION_STATUSES = ["Good", "Fair", "Poor"];
const SEEDLING_CONDITIONS = ["Healthy", "Stressed", "Diseased"];
const WATER_SOURCES = ["Rainfall", "Borehole", "River", "Irrigation", "Other"];
const YES_NO = ["Yes", "No"];

export default function NurseryInspectionScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{
    inspectorName?: string;
    stage?: string;
    grower?: string;
    gps?: string;
    deviceId?: string;
    inspectionId?: string;
    scheduledDate?: string;
    inspectionType?: string;
    editingReportId?: string;
  }>();

  const grower = useMemo(() => {
    try {
      return params.grower
        ? (JSON.parse(params.grower) as GrowerProfile)
        : null;
    } catch {
      return null;
    }
  }, [params.grower]);

  const inspectionId = (params.inspectionId ?? "").toString();
  const scheduledDate = (params.scheduledDate ?? "").toString();
  const inspectionType = (params.inspectionType ?? "").toString();

  const [inspectorName] = useState((params.inspectorName ?? "").toString());
  const [gps, setGps] = useState((params.gps ?? "").toString());
  const [deviceId, setDeviceId] = useState((params.deviceId ?? "").toString());
  const [locationError, setLocationError] = useState("");

  useEffect(() => {
    if (deviceId) return;
    (async () => {
      const id = await getDeviceId();
      setDeviceId(id);
    })();
  }, [deviceId]);

  useEffect(() => {
    if (gps.trim()) return;
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
  }, [gps]);

  const [seedVariety, setSeedVariety] = useState("");
  const [nurseryBeds, setNurseryBeds] = useState("");
  const [dateOfSowing, setDateOfSowing] = useState("");
  const [germinationStatus, setGerminationStatus] = useState("");
  const [seedlingCondition, setSeedlingCondition] = useState("");
  const [waterSource, setWaterSource] = useState("");
  const [pestDiseasePresence, setPestDiseasePresence] = useState("");
  const [pestDiseaseNotes, setPestDiseaseNotes] = useState("");
  const [fertilizerUsed, setFertilizerUsed] = useState("");
  const [fertilizerNotes, setFertilizerNotes] = useState("");
  const [chemicalsUsed, setChemicalsUsed] = useState("");
  const [chemicalsNotes, setChemicalsNotes] = useState("");
  const [inspectorRemarks, setInspectorRemarks] = useState("");

  useEffect(() => {
    if (!params.editingReportId) return;
    import("@/lib/inspection-storage").then((module) => {
      module.listInspectionReports().then((reports) => {
        const report = reports.find((r) => r.id === params.editingReportId);
        if (!report || report.stage !== "Nursery") return;
        const p = report.payload as NurseryInspection;
        setSeedVariety(p.seedVariety);
        setNurseryBeds(p.nurseryBeds);
        setDateOfSowing(p.dateOfSowing);
        setGerminationStatus(p.germinationStatus);
        setSeedlingCondition(p.seedlingCondition);
        setWaterSource(p.waterSource);
        setPestDiseasePresence(p.pestDiseasePresence);
        setPestDiseaseNotes(p.pestDiseaseNotes);
        setFertilizerUsed(p.fertilizerUsed);
        setFertilizerNotes(p.fertilizerNotes);
        setChemicalsUsed(p.chemicalsUsed);
        setChemicalsNotes(p.chemicalsNotes);
        setInspectorRemarks(p.inspectorRemarks);
      });
    });
  }, [params.editingReportId]);

  const canSubmit = useMemo(() => {
    return Boolean(
      grower &&
      inspectorName.trim() &&
      gps.trim() &&
      deviceId &&
      seedVariety.trim() &&
      nurseryBeds.trim() &&
      dateOfSowing.trim() &&
      germinationStatus &&
      seedlingCondition &&
      waterSource &&
      pestDiseasePresence &&
      fertilizerUsed &&
      chemicalsUsed &&
      inspectorRemarks.trim(),
    );
  }, [
    dateOfSowing,
    deviceId,
    germinationStatus,
    grower,
    inspectorName,
    inspectorRemarks,
    gps,
    nurseryBeds,
    pestDiseasePresence,
    fertilizerUsed,
    chemicalsUsed,
    seedVariety,
    seedlingCondition,
    waterSource,
  ]);

  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (!grower) missing.push("Grower");
    if (!inspectorName.trim()) missing.push("Inspector Name");
    if (!gps.trim()) missing.push("GPS Location");
    if (!deviceId) missing.push("Device ID");
    if (!seedVariety.trim()) missing.push("Seed Variety");
    if (!nurseryBeds.trim()) missing.push("Nursery Size / Number of beds");
    if (!dateOfSowing.trim()) missing.push("Date of Sowing");
    if (!germinationStatus) missing.push("Germination Status");
    if (!seedlingCondition) missing.push("Seedling Condition");
    if (!waterSource) missing.push("Water Source");
    if (!pestDiseasePresence) missing.push("Pest/Disease Presence");
    if (!fertilizerUsed) missing.push("Fertilizer Used");
    if (!chemicalsUsed) missing.push("Chemicals Used");
    if (!inspectorRemarks.trim()) missing.push("Inspector Remarks");
    return missing;
  }, [
    chemicalsUsed,
    dateOfSowing,
    deviceId,
    fertilizerUsed,
    germinationStatus,
    gps,
    grower,
    inspectorName,
    inspectorRemarks,
    nurseryBeds,
    pestDiseasePresence,
    seedVariety,
    seedlingCondition,
    waterSource,
  ]);

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>

        <View style={styles.section}>
          <ThemedText type="subtitle">Grower</ThemedText>
          <ThemedText>
            {grower
              ? `${grower.name} (${grower.growerId})`
              : "No grower loaded"}
          </ThemedText>
          {grower ? (
            <ThemedText>
              {grower.province} / {grower.district} • Sponsor: {grower.sponsor || "-"} • Hectarage: {grower.hectarage || 0}
            </ThemedText>
          ) : null}
        </View>

        {inspectionId ? (
          <View style={styles.section}>
            <ThemedText type="subtitle">Scheduled Inspection</ThemedText>
            <ThemedText>Inspection: {inspectionId}</ThemedText>
            {inspectionType ? <ThemedText>Type: {inspectionType}</ThemedText> : null}
            {scheduledDate ? <ThemedText>Scheduled Date: {scheduledDate}</ThemedText> : null}
            <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          </View>
        ) : (
        <View style={styles.section}>
          <ThemedText type="subtitle">Inspection Details</ThemedText>
          <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          {!gps.trim() || locationError ? (
            <ThemedText>{locationError || "Capturing GPS..."}</ThemedText>
          ) : (
            <ThemedText>GPS: {gps}</ThemedText>
          )}
        </View>
        )}

        <View style={styles.section}>
          <ThemedText type="subtitle">Inspection Form</ThemedText>
          <TextField
            label="Seed Variety"
            value={seedVariety}
            onChangeText={setSeedVariety}
            required
          />
          <TextField
            label="Nursery Size / Number of beds"
            value={nurseryBeds}
            onChangeText={setNurseryBeds}
            required
          />
          <TextField
            label="Date of Sowing"
            value={dateOfSowing}
            onChangeText={setDateOfSowing}
            placeholder="YYYY-MM-DD"
            required
          />
          <SelectField
            label="Germination Status"
            value={germinationStatus}
            onChange={setGerminationStatus}
            options={GERMINATION_STATUSES}
            required
          />
          <SelectField
            label="Seedling Condition"
            value={seedlingCondition}
            onChange={setSeedlingCondition}
            options={SEEDLING_CONDITIONS}
            required
          />
          <SelectField
            label="Water Source"
            value={waterSource}
            onChange={setWaterSource}
            options={WATER_SOURCES}
            required
          />
          <SelectField
            label="Pest/Disease Presence"
            value={pestDiseasePresence}
            onChange={setPestDiseasePresence}
            options={YES_NO}
            required
          />
          <TextField
            label="Pest/Disease Notes"
            value={pestDiseaseNotes}
            onChangeText={setPestDiseaseNotes}
            placeholder="Optional"
            multiline
          />
          <SelectField
            label="Fertilizer Used"
            value={fertilizerUsed}
            onChange={setFertilizerUsed}
            options={YES_NO}
            required
          />
          <TextField
            label="Fertilizer Notes"
            value={fertilizerNotes}
            onChangeText={setFertilizerNotes}
            placeholder="Optional"
            multiline
          />
          <SelectField
            label="Chemicals Used"
            value={chemicalsUsed}
            onChange={setChemicalsUsed}
            options={YES_NO}
            required
          />
          <TextField
            label="Chemicals Notes"
            value={chemicalsNotes}
            onChangeText={setChemicalsNotes}
            placeholder="Optional"
            multiline
          />
          <TextField
            label="Inspector Remarks"
            value={inspectorRemarks}
            onChangeText={setInspectorRemarks}
            multiline
            required
          />
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Audit Trail</ThemedText>
          <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          <ThemedText>Date & Time: {new Date().toLocaleString()}</ThemedText>
          <ThemedText>GPS: {gps || "—"}</ThemedText>
          <ThemedText>Device ID: {deviceId || "—"}</ThemedText>
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <PrimaryButton
          title="Save Inspection"
          disabled={false}
          onPress={async () => {
            if (!grower) {
              Alert.alert("Missing grower", "Grower must exist in system.");
              return;
            }
            if (!canSubmit) {
              Alert.alert(
                "Missing fields",
                `Complete all mandatory fields before saving:\n\n${missingFields.join("\n")}`,
              );
              return;
            }

            const payload: NurseryInspection = {
              seedVariety: seedVariety.trim(),
              nurseryBeds: nurseryBeds.trim(),
              dateOfSowing: dateOfSowing.trim(),
              germinationStatus,
              seedlingCondition,
              waterSource,
              pestDiseasePresence,
              pestDiseaseNotes: pestDiseaseNotes.trim(),
              fertilizerUsed,
              fertilizerNotes: fertilizerNotes.trim(),
              chemicalsUsed,
              chemicalsNotes: chemicalsNotes.trim(),
              inspectorRemarks: inspectorRemarks.trim(),
            };

            const { generateId } = await import("@/lib/inspection-storage");
            const report: InspectionReport = {
              id: params.editingReportId?.toString() || generateId(),
              stage: "Nursery",
              grower,
              audit: {
                inspectorName: inspectorName.trim(),
                submittedAt: new Date().toISOString(),
                gps: gps.trim(),
                deviceId,
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
      </View>
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
    paddingBottom: 120,
  },
  section: {
    gap: 10,
  },
  footer: {
    padding: 20,
    paddingTop: 0,
  },
});
