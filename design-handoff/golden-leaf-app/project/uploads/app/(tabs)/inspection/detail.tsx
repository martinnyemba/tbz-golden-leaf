import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Alert, ScrollView, StyleSheet, View, Pressable } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiFetchJson } from "@/lib/inspection-storage";

type InspectionDetail = {
  id: string;
  grower: string;
  grower_name?: string;
  grower_tbz_id?: string | null;
  inspector?: string;
  inspector_name?: string;
  inspection_type?: string;
  inspection_type_display?: string;
  scheduled_date?: string;
  status?: string;
  province?: string;
  district?: string;
  notes?: string;
  created_by?: string;
  created_at?: string;
  updated_at?: string;
};

type GrowerExtras = {
  nrc_number?: string;
  sponsor?: string;
  province?: string;
  district?: string;
  hectarage?: number;
};

function formatDateTime(value: string | undefined) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

function formatDateOnly(value: string | undefined) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
}

function displayFromCode(code: string | undefined) {
  const c = String(code ?? "").trim();
  if (!c) return "—";
  return c[0] + c.slice(1).toLowerCase().replace(/_/g, " ");
}

function displayCropStage(code: string | undefined) {
  const c = String(code ?? "").trim().toUpperCase();
  if (c === "MAIN_FIELD") return "Main Field";
  if (c === "TOPPING") return "Topping";
  if (c === "REAPING") return "Reaping";
  if (c === "CURING") return "Curing";
  if (c === "GRADING") return "Grading";
  if (c === "STORAGE") return "Storage";
  return displayFromCode(code);
}

function displayTobaccoType(code: string | undefined) {
  const c = String(code ?? "").trim().toUpperCase();
  if (c === "FLUE_CURED") return "Flue Cured Tobacco";
  if (c === "BURLEY") return "Burley";
  if (c === "DARK_FIRED") return "Dark Fired Tobacco";
  return displayFromCode(code);
}

function displayBarnType(code: string | undefined) {
  const c = String(code ?? "").trim().toUpperCase();
  if (c === "BULK_CURE") return "Bulk Curer Barn";
  if (!c) return "—";
  return displayFromCode(c);
}

function displaySex(code: string | undefined) {
  const c = String(code ?? "").trim().toUpperCase();
  if (c === "MALE") return "Male";
  if (c === "FEMALE") return "Female";
  return displayFromCode(code);
}

function statusBadge(status: string | undefined) {
  const s = String(status ?? "").toUpperCase();
  if (s === "SCHEDULED") return { bg: "#E8F3EE", fg: "#0B6B3A", label: "Scheduled" };
  if (s === "IN_PROGRESS") return { bg: "#FEF3C7", fg: "#92400E", label: "In Progress" };
  if (s === "COMPLETED") return { bg: "#DCFCE7", fg: "#166534", label: "Completed" };
  if (s === "CANCELLED") return { bg: "#F3F4F6", fg: "#374151", label: "Cancelled" };
  return { bg: "#F3F4F6", fg: "#374151", label: s || "—" };
}

function inspectionTypeBadge(type: string | undefined) {
  const t = String(type ?? "").toUpperCase();
  if (t === "GROWER_VALIDATION") return { bg: "#DBEAFE", fg: "#1D4ED8", label: "Grower Validation" };
  if (t === "NURSERY_INSPECTION") return { bg: "#DCFCE7", fg: "#166534", label: "Nursery Inspection" };
  if (t === "FIELD_INSPECTION") return { bg: "#E0F2FE", fg: "#075985", label: "Field Inspection" };
  if (t === "CURING_INSPECTION") return { bg: "#FEF3C7", fg: "#92400E", label: "Curing Inspection" };
  return { bg: "#F3F4F6", fg: "#374151", label: "Inspection" };
}

export default function InspectionDetailScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const params = useLocalSearchParams<{ id?: string }>();
  const id = (params.id ?? "").toString();

  const [loading, setLoading] = useState(false);
  const [item, setItem] = useState<InspectionDetail | null>(null);
  const [growerExtras, setGrowerExtras] = useState<GrowerExtras | null>(null);
  const [extraData, setExtraData] = useState<any>(null);
  const [scheduledByName, setScheduledByName] = useState("");
  const [stakeholderMap, setStakeholderMap] = useState<Record<string, string>>({});
  const [sponsorMap, setSponsorMap] = useState<Record<string, string>>({});

  const canCapture = useMemo(() => {
    if (!item?.id) return false;
    if (extraData) return false;
    const s = (item.status ?? "").toUpperCase();
    return s !== "COMPLETED" && s !== "CANCELLED";
  }, [extraData, item?.id, item?.status]);

  const captureLabel = useMemo(() => {
    const type = item?.inspection_type;
    if (type === "GROWER_VALIDATION") return "Start Grower Validation";
    if (type === "NURSERY_INSPECTION") return "Start Nursery Inspection";
    if (type === "FIELD_INSPECTION") return "Start Field Inspection";
    if (type === "CURING_INSPECTION") return "Start Curing Inspection";
    return "Start Inspection";
  }, [item?.inspection_type]);

  const captureRoute = useMemo(() => {
    const type = item?.inspection_type;
    if (type === "GROWER_VALIDATION") return "/validation";
    if (type === "NURSERY_INSPECTION") return "/inspection/nursery";
    if (type === "FIELD_INSPECTION") return "/inspection/field";
    if (type === "CURING_INSPECTION") return "/inspection/curing";
    return "/inspection";
  }, [item?.inspection_type]);

  const growerJson = useMemo(() => {
    if (!item) return "";
    const province = growerExtras?.province ?? item.province ?? "";
    const district = growerExtras?.district ?? item.district ?? "";
    const obj = {
      portalGrowerId: item.grower,
      growerId: item.grower_tbz_id ?? item.grower,
      nrc: growerExtras?.nrc_number ?? "",
      name: item.grower_name ?? "",
      sponsor: growerExtras?.sponsor ?? "",
      province,
      district,
      hectarage: typeof growerExtras?.hectarage === "number" ? growerExtras.hectarage : 0,
    };
    return JSON.stringify(obj);
  }, [growerExtras?.district, growerExtras?.hectarage, growerExtras?.nrc_number, growerExtras?.province, growerExtras?.sponsor, item]);

  const fetchDetail = useCallback(async () => {
    if (!id) {
      Alert.alert("Missing id", "Inspection id is required.");
      return;
    }
    setLoading(true);
    setItem(null);
    setGrowerExtras(null);
    setExtraData(null);
    setScheduledByName("");
    try {
      const resp = await apiFetchJson(`/api/v1/inspectorate/inspections/${encodeURIComponent(id)}/`, { method: "GET" });
      if (!resp.ok) {
        if (resp.status === 0) {
          router.push({ pathname: "/portal-login", params: { returnTo: `/inspection/detail?id=${encodeURIComponent(id)}` } } as any);
        }
        return;
      }

      const payload = JSON.parse(resp.body) as InspectionDetail;
      setItem(payload);
      setGrowerExtras(null);

      if (payload.created_by) {
        const userResp = await apiFetchJson(`/api/v1/auth/users/${encodeURIComponent(payload.created_by)}/`, { method: "GET" });
        if (userResp.ok) {
          try {
            const u = JSON.parse(userResp.body) as any;
            setScheduledByName(String(u?.full_name ?? u?.email ?? "").trim());
          } catch {}
        }
      }

      const growerResp = await apiFetchJson(`/api/v1/growers/growers/${encodeURIComponent(payload.grower)}/`, { method: "GET" });
      if (growerResp.ok) {
        try {
          const g = JSON.parse(growerResp.body) as GrowerExtras;
          setGrowerExtras(g);
        } catch {
        }
      }

      if (payload.status === "COMPLETED" || payload.status === "IN_PROGRESS") {
        let extraEndpoint = "";
        if (payload.inspection_type === "GROWER_VALIDATION") extraEndpoint = "validations";
        else if (payload.inspection_type === "NURSERY_INSPECTION") extraEndpoint = "nursery-inspections";
        else if (payload.inspection_type === "FIELD_INSPECTION") extraEndpoint = "field-inspections";
        else if (payload.inspection_type === "CURING_INSPECTION") extraEndpoint = "curing-inspections";

        if (extraEndpoint) {
          const extResp = await apiFetchJson(
            `/api/v1/inspectorate/${extraEndpoint}/?grower=${encodeURIComponent(payload.grower)}&page_size=200`,
            { method: "GET" },
          );
          if (extResp.ok) {
            const extPayload = JSON.parse(extResp.body) as any;
            const results = Array.isArray(extPayload) ? extPayload : (extPayload.results || []);
            const match = results.find((r: any) => r.inspection === payload.id);
            if (match) {
              setExtraData(match);

              if (payload.inspection_type === "GROWER_VALIDATION") {
                const [stakeholdersResp, sponsorsResp] = await Promise.all([
                  apiFetchJson("/api/v1/inspectorate/stakeholders/?page_size=250", { method: "GET" }),
                  apiFetchJson("/api/v1/growers/sponsors/?page_size=250", { method: "GET" }),
                ]);

                if (stakeholdersResp.ok) {
                  try {
                    const list = JSON.parse(stakeholdersResp.body) as any;
                    const rows = Array.isArray(list) ? list : list?.results ?? [];
                    const next: Record<string, string> = {};
                    for (const s of rows) {
                      if (s?.id) next[String(s.id)] = String(s.name ?? s.code ?? s.id);
                    }
                    setStakeholderMap(next);
                  } catch {}
                }

                if (sponsorsResp.ok) {
                  try {
                    const list = JSON.parse(sponsorsResp.body) as any;
                    const rows = Array.isArray(list) ? list : list?.results ?? [];
                    const next: Record<string, string> = {};
                    for (const s of rows) {
                      if (s?.id) next[String(s.id)] = String(s.name ?? s.code ?? s.id);
                    }
                    setSponsorMap(next);
                  } catch {}
                }
              }
            }
          }
        }
      }

    } catch {
    } finally {
      setLoading(false);
    }
  }, [id, router]);

  useFocusEffect(
    useCallback(() => {
      fetchDetail();
    }, [fetchDetail]),
  );

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.headerRow}>
          <View style={styles.headerLeft}>
            <MaterialIcons name="assignment" size={22} color={Colors[theme].primary} />
          </View>
          {loading ? <LeafLoader size={22} /> : null}
        </View>

        {!loading && !item ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <ThemedText type="defaultSemiBold">Inspection not available offline</ThemedText>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
              Connect to the internet to download the latest inspections, then try again.
            </ThemedText>
          </View>
        ) : null}

        {item ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={styles.cardHeader}>
              <ThemedText type="defaultSemiBold">Inspection Details</ThemedText>
              <View style={styles.headerBadgesRow}>
                <View style={[styles.badge, { backgroundColor: inspectionTypeBadge(item.inspection_type).bg }]}>
                  <ThemedText style={[styles.badgeText, { color: inspectionTypeBadge(item.inspection_type).fg }]}>
                    {inspectionTypeBadge(item.inspection_type).label}
                  </ThemedText>
                </View>
                <View style={[styles.badge, { backgroundColor: statusBadge(item.status).bg }]}>
                  <ThemedText style={[styles.badgeText, { color: statusBadge(item.status).fg }]}>
                    {statusBadge(item.status).label}
                  </ThemedText>
                </View>
              </View>
            </View>
            <View style={styles.kvRow}>
              <ThemedText type="defaultSemiBold" style={styles.kvKey}>Grower</ThemedText>
              <ThemedText style={styles.kvValue}>{item.grower_name ?? "—"}</ThemedText>
            </View>
            <View style={styles.kvRow}>
              <ThemedText type="defaultSemiBold" style={styles.kvKey}>Inspector</ThemedText>
              <ThemedText style={styles.kvValue}>{item.inspector_name ?? "—"}</ThemedText>
            </View>
            <View style={styles.kvRow}>
              <ThemedText type="defaultSemiBold" style={styles.kvKey}>Scheduled Date</ThemedText>
              <ThemedText style={styles.kvValue}>{formatDateOnly(item.scheduled_date)}</ThemedText>
            </View>
            <View style={styles.kvRow}>
              <ThemedText type="defaultSemiBold" style={styles.kvKey}>Province / District</ThemedText>
              <ThemedText style={styles.kvValue}>{displayFromCode(item.province)} / {item.district ?? "—"}</ThemedText>
            </View>
            {item.notes?.trim() ? (
              <View style={{ marginTop: 4 }}>
                <ThemedText type="defaultSemiBold">Notes</ThemedText>
                <View style={[styles.infoBlock, { borderColor: Colors[theme].border }]}>
                  <ThemedText>{item.notes.trim()}</ThemedText>
                </View>
              </View>
            ) : null}
          </View>
        ) : null}

        {item ? (
          <Pressable 
            style={({ pressed }) => [
              styles.card, 
              { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface, opacity: pressed ? 0.7 : 1 }
            ]}
            onPress={() => router.push({ pathname: "/registration/grower-details", params: { id: item.grower } } as any)}
          >
            <ThemedText type="defaultSemiBold">Grower</ThemedText>
            <ThemedText>{item.grower_name ?? "-"}</ThemedText>
            <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
              TBZ ID: {item.grower_tbz_id ?? "-"}
            </ThemedText>
            <MaterialIcons name="chevron-right" size={24} color={Colors[theme].muted} style={{ position: "absolute", right: 14, top: 20 }} />
          </Pressable>
        ) : null}

        {item && item.status !== "COMPLETED" && item.status !== "CANCELLED" ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={{ marginBottom: 6, flexDirection: "row", alignItems: "center", gap: 6 }}>
              <MaterialIcons name="assignment-turned-in" size={18} color={Colors[theme].primary} />
              <ThemedText type="defaultSemiBold" style={{ color: Colors[theme].primary }}>Record Inspection Data</ThemedText>
            </View>

            {item.inspection_type === "GROWER_VALIDATION" ? (
              extraData ? (
                <View style={[styles.recordedRow, { borderColor: Colors[theme].border, backgroundColor: "#F3F4F6" }]}>
                  <MaterialIcons name="check-circle" size={22} color="#0B6B3A" />
                  <View style={{ flex: 1 }}>
                    <ThemedText type="defaultSemiBold">Grower Validation Recorded</ThemedText>
                    <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                      Submitted {formatDateTime(extraData.created_at)} by {extraData.submitted_by_name ?? "—"}
                    </ThemedText>
                  </View>
                </View>
              ) : (
                <>
                  <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                    Record grower identity, crop details, barn capacity, and stakeholder presence.
                  </ThemedText>
                  <PrimaryButton
                    title="Start Grower Validation"
                    onPress={() => {
                      router.push({
                        pathname: "/validation",
                        params: {
                          inspectionId: item?.id ?? "",
                          scheduledDate: item?.scheduled_date ?? "",
                          inspectorName: item?.inspector_name ?? "",
                          inspectionType: item?.inspection_type_display ?? "",
                          grower: growerJson,
                          growerId: item?.grower ?? "",
                        },
                      } as any);
                    }}
                  />
                </>
              )
            ) : null}

            {item.inspection_type !== "GROWER_VALIDATION" && canCapture ? (
              <PrimaryButton
                title={captureLabel}
                onPress={() => {
                  router.push({
                    pathname: captureRoute,
                    params: {
                      inspectionId: item?.id ?? "",
                      scheduledDate: item?.scheduled_date ?? "",
                      inspectorName: item?.inspector_name ?? "",
                      inspectionType: item?.inspection_type_display ?? "",
                      grower: growerJson,
                      growerId: item?.grower ?? "",
                    },
                  } as any);
                }}
              />
            ) : null}
          </View>
        ) : null}

        {extraData && item?.inspection_type === "GROWER_VALIDATION" ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
              <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
                <MaterialIcons name="verified" size={18} color="#1D4ED8" />
                <ThemedText type="defaultSemiBold">Grower Validation Record</ThemedText>
              </View>
              <View style={[styles.badge, { backgroundColor: "#DCFCE7" }]}>
                <ThemedText style={[styles.badgeText, { color: "#166534" }]}>Completed</ThemedText>
              </View>
            </View>

            <View style={styles.row}><ThemedText type="defaultSemiBold">NRC Number</ThemedText><ThemedText>{extraData.nrc_number || "—"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Sex</ThemedText><ThemedText>{displaySex(extraData.sex)}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Crop Stage</ThemedText><ThemedText>{displayCropStage(extraData.crop_stage)}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Tobacco Type</ThemedText><ThemedText>{displayTobaccoType(extraData.tobacco_type)}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Tobacco Variety</ThemedText><ThemedText>{extraData.tobacco_variety || "—"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Validated Hectarage</ThemedText><ThemedText type="defaultSemiBold">{extraData.validated_hectarage} ha</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Yield / Ha</ThemedText><ThemedText>{extraData.yield_per_hectare} kg</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Sponsor</ThemedText><ThemedText>{extraData.sponsor ? (sponsorMap[String(extraData.sponsor)] ?? String(extraData.sponsor)) : "Self-Sponsored"}</ThemedText></View>
            <View style={styles.row}>
              <ThemedText type="defaultSemiBold">Risk Score</ThemedText>
              <ThemedText type="defaultSemiBold" style={{ color: extraData.risk_score >= 70 ? "#DC2626" : extraData.risk_score >= 40 ? "#D97706" : "#0B6B3A" }}>
                {extraData.risk_score} / 100
              </ThemedText>
            </View>
            <View style={styles.row}>
              <ThemedText type="defaultSemiBold">Barns</ThemedText>
              <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                <ThemedText>{extraData.number_of_barns} × {displayBarnType(extraData.barn_type)}</ThemedText>
                <View style={[styles.badge, { backgroundColor: extraData.barn_capacity_sufficient ? "#DCFCE7" : "#FEF3C7" }]}>
                  <ThemedText style={[styles.badgeText, { color: extraData.barn_capacity_sufficient ? "#166534" : "#92400E" }]}>
                    {extraData.barn_capacity_sufficient ? "Sufficient" : "Insufficient"}
                  </ThemedText>
                </View>
              </View>
            </View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Province / District</ThemedText><ThemedText>{displayFromCode(extraData.province)} / {extraData.district || "—"}</ThemedText></View>

            <View style={{ marginTop: 6 }}>
              <ThemedText type="defaultSemiBold">Stakeholders Present</ThemedText>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 6, marginTop: 6 }}>
                {(Array.isArray(extraData.stakeholders_present) ? extraData.stakeholders_present : []).length > 0 ? (
                  (extraData.stakeholders_present as any[]).map((sid) => (
                    <View key={String(sid)} style={[styles.chip, { borderColor: Colors[theme].border }]}>
                      <ThemedText style={styles.chipText}>{stakeholderMap[String(sid)] ?? String(sid)}</ThemedText>
                    </View>
                  ))
                ) : (
                  <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>None recorded</ThemedText>
                )}
              </View>
            </View>

            {extraData.inspector_remarks ? (
              <View style={{ marginTop: 10 }}>
                <ThemedText type="defaultSemiBold">Inspector Remarks</ThemedText>
                <View style={[styles.infoBlock, { borderColor: Colors[theme].border }]}>
                  <ThemedText>{String(extraData.inspector_remarks)}</ThemedText>
                </View>
              </View>
            ) : null}

            <View style={{ marginTop: 8 }}>
              <ThemedText type="defaultSemiBold">GPS</ThemedText>
              <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                {extraData.gps_latitude ? `${extraData.gps_latitude}, ${extraData.gps_longitude}` : "Not captured"}
              </ThemedText>
            </View>

            <View style={{ marginTop: 6 }}>
              <ThemedText type="defaultSemiBold">Audit</ThemedText>
              <ThemedText style={styles.small} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                Submitted by {extraData.submitted_by_name ?? "—"} on {formatDateTime(extraData.created_at)}
              </ThemedText>
            </View>
          </View>
        ) : null}

        {extraData && item?.inspection_type === "NURSERY_INSPECTION" ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={{ marginBottom: 6 }}><ThemedText type="defaultSemiBold" style={{ color: Colors[theme].primary }}>Nursery Details</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Seed Variety</ThemedText><ThemedText>{extraData.seed_variety || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Nursery Beds</ThemedText><ThemedText>{extraData.nursery_size_beds || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Date of Sowing</ThemedText><ThemedText>{extraData.date_of_sowing || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Germination Status</ThemedText><ThemedText>{extraData.germination_status || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Seedling Cond.</ThemedText><ThemedText>{extraData.seedling_condition || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Water Source</ThemedText><ThemedText>{extraData.water_source || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Pests/Diseases</ThemedText><ThemedText>{extraData.pest_disease_present ? "Present" : "None"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Fertilizer Used</ThemedText><ThemedText>{extraData.fertilizer_used ? "Yes" : "No"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Chemicals Used</ThemedText><ThemedText>{extraData.chemicals_used ? "Yes" : "No"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Inspector Remarks</ThemedText><ThemedText style={{ flexShrink: 1, textAlign: 'right', paddingLeft: 10 }}>{extraData.inspector_remarks || "-"}</ThemedText></View>
          </View>
        ) : null}

        {extraData && item?.inspection_type === "FIELD_INSPECTION" ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={{ marginBottom: 6 }}><ThemedText type="defaultSemiBold" style={{ color: Colors[theme].primary }}>Field Details</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Transplanted Ha</ThemedText><ThemedText>{extraData.transplanted_hectarage} ha</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Crop Stage</ThemedText><ThemedText>{extraData.crop_stage || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Plant Population</ThemedText><ThemedText>{extraData.plant_population || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Crop Uniformity</ThemedText><ThemedText>{extraData.crop_uniformity || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Fertilizer Application</ThemedText><ThemedText>{extraData.fertilizer_application || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Pest / Disease</ThemedText><ThemedText>{extraData.pest_disease_status || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Weed Control</ThemedText><ThemedText>{extraData.weed_control || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Irrigation</ThemedText><ThemedText>{extraData.irrigation_status || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Inspector Remarks</ThemedText><ThemedText style={{ flexShrink: 1, textAlign: 'right', paddingLeft: 10 }}>{extraData.inspector_remarks || "-"}</ThemedText></View>
          </View>
        ) : null}

        {extraData && item?.inspection_type === "CURING_INSPECTION" ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={{ marginBottom: 6 }}><ThemedText type="defaultSemiBold" style={{ color: Colors[theme].primary }}>Curing Details</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Number of Barns</ThemedText><ThemedText>{extraData.number_of_barns || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Barn Type</ThemedText><ThemedText>{extraData.barn_type || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Curing Cycles</ThemedText><ThemedText>{extraData.curing_cycles || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Fuel Source</ThemedText><ThemedText>{extraData.fuel_source || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Curing Status</ThemedText><ThemedText>{extraData.curing_status || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Leaf Quality</ThemedText><ThemedText>{extraData.leaf_quality || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Grading Status</ThemedText><ThemedText>{extraData.grading_status || "-"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Inspector Remarks</ThemedText><ThemedText style={{ flexShrink: 1, textAlign: 'right', paddingLeft: 10 }}>{extraData.inspector_remarks || "-"}</ThemedText></View>
          </View>
        ) : null}

        {item ? (
          <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <View style={{ marginBottom: 6, flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <MaterialIcons name="shield" size={18} color={Colors[theme].primary} />
              <ThemedText type="defaultSemiBold" style={{ color: Colors[theme].primary }}>Audit Trail</ThemedText>
            </View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Scheduled by</ThemedText><ThemedText>{scheduledByName || "—"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Created on</ThemedText><ThemedText>{formatDateTime(item.created_at)}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Last updated</ThemedText><ThemedText>{formatDateTime(item.updated_at)}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Inspector</ThemedText><ThemedText>{item.inspector_name ?? "—"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Type</ThemedText><ThemedText>{item.inspection_type_display ?? "—"}</ThemedText></View>
            <View style={styles.row}><ThemedText type="defaultSemiBold">Status</ThemedText><ThemedText>{statusBadge(item.status).label}</ThemedText></View>
            <View style={{ height: 1, backgroundColor: Colors[theme].border, marginVertical: 4 }} />
            <View style={styles.row}>
              <ThemedText type="defaultSemiBold">Data Captured</ThemedText>
              <View style={{ backgroundColor: extraData ? Colors[theme].primary : Colors[theme].border, paddingHorizontal: 8, paddingVertical: 2, borderRadius: 12 }}>
                <ThemedText style={{ fontSize: 12, color: extraData ? Colors[theme].surface : Colors[theme].text }}>{extraData ? "Yes" : "Pending"}</ThemedText>
              </View>
            </View>
          </View>
        ) : null}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 20, gap: 14, paddingBottom: 40 },
  headerRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  headerLeft: { flexDirection: "row", alignItems: "center", gap: 10 },
  card: { borderWidth: 1, borderRadius: 16, padding: 14, gap: 8, overflow: "hidden" },
  cardHeader: { gap: 10 },
  headerBadgesRow: { flexDirection: "row", flexWrap: "wrap", justifyContent: "flex-end", gap: 8 },
  kvRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", gap: 12 },
  kvKey: { flex: 1 },
  kvValue: { flexShrink: 1, maxWidth: "58%", textAlign: "right" },
  row: { flexDirection: "row", justifyContent: "space-between" },
  small: { fontSize: 12, lineHeight: 16 },
  badge: { paddingHorizontal: 10, paddingVertical: 6, borderRadius: 999 },
  badgeText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  infoBlock: { borderWidth: 1, borderRadius: 12, padding: 12, marginTop: 8, overflow: "hidden" },
  chip: { borderWidth: 1, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6, backgroundColor: "#F9FAFB", overflow: "hidden" },
  chipText: { fontSize: 12 },
  recordedRow: { flexDirection: "row", alignItems: "center", gap: 10, padding: 12, borderWidth: 1, borderRadius: 12, marginTop: 6, overflow: "hidden" },
});
