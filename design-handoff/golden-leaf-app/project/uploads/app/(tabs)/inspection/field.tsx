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
    type FieldInspection,
    type GrowerProfile,
    type InspectionReport,
} from "@/lib/inspection-storage";

const CROP_STAGES = ["Main Field", "Topping", "Reaping", "Curing", "Grading", "Storage"];
const PLANT_POPULATION = ["Full", "Acceptable", "Poor"];
const CROP_UNIFORMITY = ["Uniform", "Moderate", "Variable"];
const FERTILIZER_APPLICATION = ["Adequate", "Inadequate", "Excessive", "None"];
const PEST_DISEASE_STATUS = ["None", "Low", "Moderate", "High"];
const WEED_CONTROL = ["Good", "Fair", "Poor"];
const IRRIGATION_STATUS = ["Adequate", "Inadequate", "Not Applicable"];

export default function FieldInspectionScreen() {
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

  const [transplantedHectarage, setTransplantedHectarage] = useState("");
  const [cropStage, setCropStage] = useState("");
  const [plantPopulation, setPlantPopulation] = useState("");
  const [cropUniformity, setCropUniformity] = useState("");
  const [fertilizerApplication, setFertilizerApplication] = useState("");
  const [pestDiseaseStatus, setPestDiseaseStatus] = useState("");
  const [weedControl, setWeedControl] = useState("");
  const [irrigationStatus, setIrrigationStatus] = useState("");
  const [inspectorRemarks, setInspectorRemarks] = useState("");

  useEffect(() => {
    if (!params.editingReportId) return;
    import("@/lib/inspection-storage").then((module) => {
      module.listInspectionReports().then((reports) => {
        const report = reports.find((r) => r.id === params.editingReportId);
        if (!report || report.stage !== "Field") return;
        const p = report.payload as FieldInspection;
        setTransplantedHectarage(p.transplantedHectarage);
        setCropStage(p.cropStage);
        setPlantPopulation(p.plantPopulation);
        setCropUniformity(p.cropUniformity);
        setFertilizerApplication(p.fertilizerApplication);
        setPestDiseaseStatus(p.pestDiseaseStatus);
        setWeedControl(p.weedControl);
        setIrrigationStatus(p.irrigationStatus);
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
      transplantedHectarage.trim() &&
      cropStage &&
      plantPopulation &&
      cropUniformity &&
      fertilizerApplication &&
      pestDiseaseStatus &&
      weedControl &&
      irrigationStatus &&
      inspectorRemarks.trim(),
    );
  }, [
    cropStage,
    cropUniformity,
    deviceId,
    fertilizerApplication,
    gps,
    grower,
    inspectorName,
    inspectorRemarks,
    irrigationStatus,
    pestDiseaseStatus,
    plantPopulation,
    transplantedHectarage,
    weedControl,
  ]);

  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (!grower) missing.push("Grower");
    if (!inspectorName.trim()) missing.push("Inspector Name");
    if (!gps.trim()) missing.push("GPS Coordinates");
    if (!deviceId) missing.push("Device ID");
    if (!transplantedHectarage.trim()) missing.push("Transplanted Hectarage");
    if (!cropStage) missing.push("Crop Stage");
    if (!plantPopulation) missing.push("Plant Population");
    if (!cropUniformity) missing.push("Crop Uniformity");
    if (!fertilizerApplication) missing.push("Fertilizer Application");
    if (!pestDiseaseStatus) missing.push("Pest/Disease Status");
    if (!weedControl) missing.push("Weed Control");
    if (!irrigationStatus) missing.push("Irrigation Status");
    if (!inspectorRemarks.trim()) missing.push("Inspector Remarks");
    return missing;
  }, [
    cropStage,
    cropUniformity,
    deviceId,
    fertilizerApplication,
    gps,
    grower,
    inspectorName,
    inspectorRemarks,
    irrigationStatus,
    pestDiseaseStatus,
    plantPopulation,
    transplantedHectarage,
    weedControl,
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

        <View style={styles.section}>
          <ThemedText type="subtitle">{inspectionId ? "Scheduled Inspection" : "Inspection Context"}</ThemedText>
          {inspectionId ? <ThemedText>Inspection: {inspectionId}</ThemedText> : null}
          {inspectionType ? <ThemedText>Type: {inspectionType}</ThemedText> : null}
          {scheduledDate ? <ThemedText>Scheduled Date: {scheduledDate}</ThemedText> : null}
          <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          {!gps.trim() || locationError ? (
            <ThemedText>{locationError || "Capturing GPS..."}</ThemedText>
          ) : (
            <ThemedText>GPS: {gps}</ThemedText>
          )}
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Inspection Form</ThemedText>
          <TextField
            label="Transplanted Hectarage"
            value={transplantedHectarage}
            onChangeText={setTransplantedHectarage}
            keyboardType="decimal-pad"
            required
          />
          <SelectField
            label="Crop Stage"
            value={cropStage}
            onChange={setCropStage}
            options={CROP_STAGES}
            required
          />
          <SelectField
            label="Plant Population"
            value={plantPopulation}
            onChange={setPlantPopulation}
            options={PLANT_POPULATION}
            required
          />
          <SelectField
            label="Crop Uniformity"
            value={cropUniformity}
            onChange={setCropUniformity}
            options={CROP_UNIFORMITY}
            required
          />
          <SelectField
            label="Fertilizer Application"
            value={fertilizerApplication}
            onChange={setFertilizerApplication}
            options={FERTILIZER_APPLICATION}
            required
          />
          <SelectField
            label="Pest/Disease Status"
            value={pestDiseaseStatus}
            onChange={setPestDiseaseStatus}
            options={PEST_DISEASE_STATUS}
            required
          />
          <SelectField
            label="Weed Control"
            value={weedControl}
            onChange={setWeedControl}
            options={WEED_CONTROL}
            required
          />
          <SelectField
            label="Irrigation Status"
            value={irrigationStatus}
            onChange={setIrrigationStatus}
            options={IRRIGATION_STATUS}
            required
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

            const payload: FieldInspection = {
              transplantedHectarage: transplantedHectarage.trim(),
              cropStage,
              plantPopulation,
              cropUniformity,
              fertilizerApplication,
              pestDiseaseStatus,
              weedControl,
              irrigationStatus,
              inspectorRemarks: inspectorRemarks.trim(),
            };

            const { generateId } = await import("@/lib/inspection-storage");
            const report: InspectionReport = {
              id: params.editingReportId?.toString() || generateId(),
              stage: "Field",
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
