import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import * as ImagePicker from "expo-image-picker";
import * as Location from "expo-location";
import { CameraView, useCameraPermissions } from "expo-camera";
import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useEffect, useMemo, useState, useRef } from "react";
import {
  Alert,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from "react-native";
import DateTimePicker from "@react-native-community/datetimepicker";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { SelectField, TextField } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { COUNTRY_DIAL_CODES } from "@/constants/countries";

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
const API_BASE_URL_KEY = "tbz:portalBaseUrl:v1";
const AUTH_ACCESS_KEY = "tbz:portalAccessToken:v1";
const AUTH_REFRESH_KEY = "tbz:portalRefreshToken:v1";

const SEXES = ["Male", "Female"];
const CATEGORIES = ["Small Scale", "Commercial", "Company"];
const PHONE_CODES = COUNTRY_DIAL_CODES.map(c => `${c.flag} ${c.name} (${c.code})`);
const PROVINCES = [
  "Central", "Copperbelt", "Eastern", "Luapula", "Lusaka", 
  "Muchinga", "Northern", "North-Western", "Southern", "Western"
];
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
const TOBACCO_ZM_GREEN = "#0B6B3A";

type GrowerApi = {
  grower_type?: GrowerTypeCode;
  first_name?: string;
  middle_name?: string;
  last_name?: string;
  nrc_number?: string;
  sex?: string;
  phone_number?: string;
  email?: string;
  date_of_birth?: string;
  address?: string;
  town_or_village?: string;
  province?: string;
  district?: string;
  gps_latitude?: string | number | null;
  gps_longitude?: string | number | null;
};

function auditDateTimeNow() {
  return new Date().toLocaleString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function uriLabel(uri: string) {
  const last = uri.split("/").filter(Boolean).pop();
  return last ?? "Captured";
}

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

function normalizeId(raw: string) {
  return raw.trim().replace(/\s+/g, "").toLowerCase();
}

function WizardStepper({ step }: { step: 1 | 2 }) {
  return (
    <View style={styles.stepper}>
      <View
        style={[
          styles.stepItem,
          step === 1 ? styles.stepActive : step > 1 ? styles.stepDone : null,
        ]}
      >
        <View
          style={[
            styles.stepIndex,
            step === 1 ? styles.stepIndexActive : step > 1 ? styles.stepIndexDone : null,
          ]}
        >
          {step > 1 ? (
            <MaterialIcons name="check" size={16} color="#FFFFFF" />
          ) : (
            <ThemedText style={styles.stepIndexText} lightColor="#FFFFFF" darkColor="#FFFFFF">
              1
            </ThemedText>
          )}
        </View>
        <ThemedText type="defaultSemiBold">Personal Info</ThemedText>
      </View>

      <View style={[styles.stepItem, step === 2 ? styles.stepActive : null]}>
        <View style={[styles.stepIndex, step === 2 ? styles.stepIndexActive : null]}>
          <ThemedText style={styles.stepIndexText} lightColor="#FFFFFF" darkColor="#FFFFFF">
            2
          </ThemedText>
        </View>
        <ThemedText type="defaultSemiBold">Crop Info</ThemedText>
      </View>
    </View>
  );
}

function ProgressBar({ step }: { step: 1 | 2 }) {
  return (
    <View style={styles.progressTrack}>
      <View
        style={[
          styles.progressFill,
          { width: step === 1 ? "50%" : "100%" },
        ]}
      />
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
    <ThemedText
      style={styles.chipText}
      lightColor="#0F3D25"
      darkColor="#0F3D25"
    >
      {label}
    </ThemedText>
  );
}

function GrowerTypeRadio({
  value,
  onChange,
}: {
  value: GrowerTypeCode | "";
  onChange: (next: GrowerTypeCode) => void;
}) {
  return (
    <View style={styles.radioGroup}>
      {(
        [
          { code: "INDIVIDUAL" as const, label: "Individual", icon: "person" as const },
          { code: "COMPANY" as const, label: "Company", icon: "business" as const },
        ] as const
      ).map((item) => {
        const selected = value === item.code;
        return (
          <Pressable
            key={item.code}
            onPress={() => onChange(item.code)}
            style={[
              styles.radioItem,
              {
                borderColor: selected ? TOBACCO_ZM_GREEN : "#E5E7EB",
                backgroundColor: selected ? "#E8F3EE" : "transparent",
              },
            ]}
          >
            <MaterialIcons
              name={item.icon}
              size={18}
              color={selected ? TOBACCO_ZM_GREEN : "#6B7280"}
            />
            <ThemedText type="defaultSemiBold">{item.label}</ThemedText>
          </Pressable>
        );
      })}
    </View>
  );
}

function FileDrop({
  label,
  icon,
  pickedUri,
  onPick,
}: {
  label: string;
  icon: "photo-camera" | "credit-card";
  pickedUri: string;
  onPick: () => void;
}) {
  const display = pickedUri ? uriLabel(pickedUri) : label;
  return (
    <Pressable onPress={onPick} style={styles.fileDrop}>
      <MaterialIcons name={icon} size={26} color={TOBACCO_ZM_GREEN} />
      <ThemedText type="defaultSemiBold" style={styles.fileDropLabel}>
        {display}
      </ThemedText>
      <ThemedText style={styles.fileDropHint} lightColor="#6B7280" darkColor="#6B7280">
        JPG, PNG up to 5 MB
      </ThemedText>
    </Pressable>
  );
}

export default function RegistrationNewScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [auditDateTime, setAuditDateTime] = useState(() => auditDateTimeNow());

  const [growerType, setGrowerType] = useState<GrowerTypeCode | "">("");
  const [firstName, setFirstName] = useState("");
  const [middleName, setMiddleName] = useState("");
  const [lastName, setLastName] = useState("");
  const [nrcNumber, setNrcNumber] = useState("");
  const [sex, setSex] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [category, setCategory] = useState("Small Scale");
  const [phoneCountryCode, setPhoneCountryCode] = useState("🇿🇲 ZMB (260)");
  const [phoneNumberLocal, setPhoneNumberLocal] = useState("");

  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [townOrVillage, setTownOrVillage] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");

  const [gpsLatitude, setGpsLatitude] = useState("");
  const [gpsLongitude, setGpsLongitude] = useState("");
  const [locationError, setLocationError] = useState("");
  const [showDatePicker, setShowDatePicker] = useState(false);

  const [profilePhotoUri, setProfilePhotoUri] = useState("");
  const [idFrontUri, setIdFrontUri] = useState("");
  const [idBackUri, setIdBackUri] = useState("");
  const [editingId, setEditingId] = useState<string | undefined>();
  const [showInlineCamera, setShowInlineCamera] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<"front" | "back">("back");
  const [cameraTarget, setCameraTarget] = useState<((uri: string) => void) | null>(null);
  const cameraRef = useRef<CameraView>(null);
  const [cameraPermission, requestCameraPermission] = useCameraPermissions();

  const [portalBaseUrl, setPortalBaseUrl] = useState(
    sanitizePortalBaseUrl(
      process.env.EXPO_PUBLIC_TBZ_TRMS_BASE_URL ||
        (__DEV__ ? (Platform.OS === "android" ? "http://10.0.2.2:8000/" : "http://127.0.0.1:8000/") : "https://trms.tbz.co.zm/"),
    ),
  );
  const [accessToken, setAccessToken] = useState("");
  const [refreshToken, setRefreshToken] = useState("");
  const [authOpen, setAuthOpen] = useState(false);
  const [authEmail, setAuthEmail] = useState("");
  const [authPassword, setAuthPassword] = useState("");
  const [authLoading, setAuthLoading] = useState(false);


  const resetForm = useCallback(() => {
    setGrowerType("");
    setFirstName("");
    setMiddleName("");
    setLastName("");
    setNrcNumber("");
    setSex("");
    setDateOfBirth("");
    setCategory("Small Scale");
    setPhoneCountryCode("🇿🇲 ZMB (260)");
    setPhoneNumberLocal("");
    setEmail("");
    setAddress("");
    setTownOrVillage("");
    setProvince("");
    setDistrict("");
    setGpsLatitude("");
    setGpsLongitude("");
    setLocationError("");
    setProfilePhotoUri("");
    setIdFrontUri("");
    setIdBackUri("");
    setAuditDateTime(auditDateTimeNow());
    setEditingId(undefined);
  }, []);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        const savedBaseUrl = await AsyncStorage.getItem(API_BASE_URL_KEY);
        if (!cancelled && savedBaseUrl) setPortalBaseUrl(sanitizePortalBaseUrl(savedBaseUrl));
        const savedAccess = await AsyncStorage.getItem(AUTH_ACCESS_KEY);
        const savedRefresh = await AsyncStorage.getItem(AUTH_REFRESH_KEY);
        if (!cancelled && savedAccess) setAccessToken(savedAccess);
        if (!cancelled && savedRefresh) setRefreshToken(savedRefresh);

        const clearFlag = await AsyncStorage.getItem(CLEAR_FORM_KEY);
        if (clearFlag) {
          await AsyncStorage.multiRemove([CLEAR_FORM_KEY, DRAFT_KEY]);
          if (!cancelled) resetForm();
          return;
        }

        const raw = await AsyncStorage.getItem(DRAFT_KEY);
        if (!raw) return;
        try {
          const parsed = JSON.parse(raw) as GrowerRegistrationDraft;
          if (cancelled) return;
          setGrowerType(parsed.growerType);
          setFirstName(parsed.firstName);
          setMiddleName(parsed.middleName);
          setLastName(parsed.lastName);
          setNrcNumber(parsed.nrcNumber);
          setSex(parsed.sex);
          setDateOfBirth(parsed.dateOfBirth);
          setCategory(parsed.category ?? "Small Scale");
          
          let rawPhone = parsed.phoneNumber || "";
          let matchedOption = "🇿🇲 ZMB (260)";
          const sorted = [...COUNTRY_DIAL_CODES].sort((a,b) => b.code.length - a.code.length);
          for (const cc of sorted) {
            if (rawPhone.startsWith(cc.code)) {
              matchedOption = `${cc.flag} ${cc.name} (${cc.code})`;
              rawPhone = rawPhone.slice(cc.code.length);
              break;
            }
          }
          setPhoneCountryCode(matchedOption);
          setPhoneNumberLocal(rawPhone);

          setEmail(parsed.email);
          setAddress(parsed.address);
          setTownOrVillage(parsed.townOrVillage);
          setProvince(parsed.province);
          setDistrict(parsed.district);
          setGpsLatitude(parsed.gpsLatitude);
          setGpsLongitude(parsed.gpsLongitude);
          setProfilePhotoUri(parsed.profilePhotoUri);
          setIdFrontUri(parsed.idFrontUri);
          setIdBackUri(parsed.idBackUri);
          setAuditDateTime(parsed.auditDateTime);
          setEditingId(parsed.editingId);
        } catch {
          await AsyncStorage.removeItem(DRAFT_KEY);
        }
      })();
      return () => {
        cancelled = true;
      };
    }, [resetForm]),
  );

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

  const districts = useMemo(
    () => (province ? DISTRICTS[province] ?? [] : []),
    [province],
  );

  useEffect(() => {
    if (!district) return;
    if (districts.includes(district)) return;
    setDistrict("");
  }, [district, districts]);

  async function refreshAccessToken(baseUrl: string, token: string) {
    const resp = await fetch(`${runtimePortalBaseUrl(baseUrl)}/api/v1/auth/token/refresh/`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh: token }),
    });
    if (!resp.ok) return null;
    const payload = (await resp.json()) as { access?: string };
    const next = payload.access ?? "";
    return next || null;
  }

  async function apiFetch(path: string, init: RequestInit = {}) {
    const baseUrl = runtimePortalBaseUrl(portalBaseUrl);
    const url = `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
    const headers = new Headers(init.headers);
    if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

    const first = await fetch(url, { ...init, headers });
    if (first.status !== 401) return first;
    if (!refreshToken) return first;

    const nextAccess = await refreshAccessToken(baseUrl, refreshToken);
    if (!nextAccess) return first;
    setAccessToken(nextAccess);
    await AsyncStorage.setItem(AUTH_ACCESS_KEY, nextAccess);

    headers.set("Authorization", `Bearer ${nextAccess}`);
    return fetch(url, { ...init, headers });
  }

  async function loginToPortal() {
    const baseUrl = runtimePortalBaseUrl(portalBaseUrl);
    if (!baseUrl) {
      Alert.alert("Missing portal URL", "Enter the portal base URL first.");
      return;
    }
    if (!authEmail.trim() || !authPassword) {
      Alert.alert("Missing credentials", "Enter email and password.");
      return;
    }

    setAuthLoading(true);
    try {
      await AsyncStorage.setItem(API_BASE_URL_KEY, sanitizePortalBaseUrl(portalBaseUrl));
      setPortalBaseUrl(sanitizePortalBaseUrl(portalBaseUrl));

      const email = authEmail.trim();
      const password = authPassword;

      const bodyArgs1: any = { email, password };
      const bodyArgs2: any = { username: email, password };

      let resp = await fetch(`${baseUrl}/api/v1/auth/login/`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bodyArgs1),
      });

      if (!resp.ok && (resp.status === 400 || resp.status === 401)) {
        resp = await fetch(`${baseUrl}/api/v1/auth/login/`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(bodyArgs2),
        });
      }

      if (!resp.ok) {
        const raw = await resp.text();
        let errMsg = "Network request failed";
        try {
          const payload = JSON.parse(raw);
          if (payload?.otp_required || raw.includes('otp_required')) {
             setAuthOpen(false); // Close the modal
             setAuthLoading(false);
             router.push({ pathname: "/login-2fa", params: { email, password, baseUrl, returnTo: "registration" } });
             return;
          }
          errMsg = payload?.otp_code || payload?.error?.message || payload?.detail || payload?.message || raw.trim().slice(0, 400);
        } catch {
          errMsg = raw.trim() ? raw.slice(0, 400) : "No response body.";
        }
        Alert.alert("Login failed", errMsg);
        return;
      }

      const payload = (await resp.json()) as { access?: string; refresh?: string };
      const nextAccess = payload.access ?? "";
      const nextRefresh = payload.refresh ?? "";
      if (!nextAccess || !nextRefresh) {
        Alert.alert("Login failed", "Portal did not return tokens.");
        return;
      }

      setAccessToken(nextAccess);
      setRefreshToken(nextRefresh);
      await AsyncStorage.setItem(AUTH_ACCESS_KEY, nextAccess);
      await AsyncStorage.setItem(AUTH_REFRESH_KEY, nextRefresh);

      setAuthOpen(false);
      setAuthPassword("");

    } catch {
      Alert.alert("Login failed", "Network error while logging in.");
    } finally {
      setAuthLoading(false);
    }
  }

  async function takePicture(
    cameraType: "front" | "back",
    setUri: (uri: string) => void,
  ) {
    if (!cameraPermission?.granted) {
      const res = await requestCameraPermission();
      if (!res.granted) {
        Alert.alert("Permission denied", "We need camera permission to take a picture.");
        return;
      }
    }
    setCameraFacing(cameraType);
    setCameraTarget(() => setUri);
    setShowInlineCamera(true);
  }

  async function pickImage(setUri: (uri: string) => void) {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) {
      Alert.alert("Permission denied", "Enable media library permission to upload images.");
      return;
    }
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        quality: 0.5,
        allowsEditing: false,
      });
      if (result.canceled) return;
      const uri = result.assets[0]?.uri;
      if (uri) setUri(uri);
    } catch (e) {
      Alert.alert("Gallery error", "Failed to open gallery.");
    }
  }

  function handleMediaSelection(cameraType: "front" | "back", setUri: (uri: string) => void) {
    Alert.alert(
      "Select Source",
      "Would you like to take a photo or upload from your gallery?",
      [
        { text: "Camera", onPress: () => takePicture(cameraType, setUri) },
        { text: "Gallery", onPress: () => pickImage(setUri) },
        { text: "Cancel", style: "cancel" },
      ],
      { cancelable: true }
    );
  }

  return (
    <ThemedView style={styles.container}>
      {showInlineCamera ? (
        <View style={{ flex: 1, backgroundColor: "#000" }}>
          <View style={{ flex: 1 }}>
            <CameraView
              style={StyleSheet.absoluteFillObject}
              facing={cameraFacing}
              ref={cameraRef}
            />
            <View
              style={[
                StyleSheet.absoluteFillObject,
                { backgroundColor: "transparent", flexDirection: "column", justifyContent: "space-between", padding: 24 },
              ]}
              pointerEvents="box-none"
            >
              <Pressable
                style={{ alignSelf: "flex-end", padding: 12, backgroundColor: "rgba(0,0,0,0.5)", borderRadius: 24 }}
                onPress={() => setShowInlineCamera(false)}
              >
                <MaterialIcons name="close" size={24} color="#FFF" />
              </Pressable>

              <View style={{ flexDirection: "row", justifyContent: "space-around", alignItems: "center", marginBottom: 40 }}>
                <Pressable
                  style={{ padding: 12, backgroundColor: "rgba(0,0,0,0.5)", borderRadius: 24 }}
                  onPress={() => setCameraFacing(f => f === "back" ? "front" : "back")}
                >
                  <MaterialIcons name="flip-camera-android" size={28} color="#FFF" />
                </Pressable>

                <Pressable
                  style={{ width: 72, height: 72, borderRadius: 36, backgroundColor: "#FFF", borderWidth: 4, borderColor: "rgba(0,0,0,0.5)", justifyContent: "center", alignItems: "center" }}
                  onPress={async () => {
                    if (cameraRef.current) {
                      const photo = await cameraRef.current.takePictureAsync({ quality: 0.5 });
                      if (photo && cameraTarget) {
                        cameraTarget(photo.uri);
                        setShowInlineCamera(false);
                      }
                    }
                  }}
                >
                  <MaterialIcons name="camera" size={32} color="#000" />
                </Pressable>

                <View style={{ width: 52 }} />
              </View>
            </View>
          </View>
        </View>
      ) : (
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.wizardShell}>
          <Modal visible={authOpen} transparent animationType="slide" onRequestClose={() => setAuthOpen(false)}>
            <Pressable style={styles.modalBackdrop} onPress={() => setAuthOpen(false)} />
            <View style={styles.modalSheet}>
              <View style={styles.modalHeader}>
                <ThemedText type="subtitle">Portal Login</ThemedText>
              </View>
              <View style={styles.modalBody}>
                <TextField
                  label="Portal Base URL"
                  value={portalBaseUrl}
                  onChangeText={setPortalBaseUrl}
                  placeholder="https://example.com"
                  required
                />
                <TextField
                  label="Email"
                  value={authEmail}
                  onChangeText={setAuthEmail}
                  placeholder="name@example.com"
                  required
                />
                <TextField
                  label="Password"
                  value={authPassword}
                  onChangeText={setAuthPassword}
                  secureTextEntry
                  required
                />
                <Pressable
                  style={[styles.modalAction, { opacity: authLoading ? 0.7 : 1 }]}
                  disabled={authLoading}
                  onPress={loginToPortal}
                >
                  {authLoading ? (
                    <LeafLoader size={20} color="#FFFFFF" />
                  ) : (
                    <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">
                      Login
                    </ThemedText>
                  )}
                </Pressable>
              </View>
            </View>
          </Modal>

          <WizardStepper step={1} />
          <ProgressBar step={1} />

          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <ThemedText type="defaultSemiBold" style={styles.cardTitle}>
                Step 1 — Personal & Location Details
              </ThemedText>
              <ThemedText style={styles.cardSubtitle} lightColor={Colors.light.muted} darkColor={Colors.dark.muted}>
                Fields marked with * are required.
              </ThemedText>
            </View>

            <View style={styles.cardBody}>
              <ThemedText type="defaultSemiBold">Grower Type *</ThemedText>
              <GrowerTypeRadio value={growerType} onChange={setGrowerType} />

              <View style={styles.gridRow}>
                <View style={styles.gridCol}>
                  <TextField
                    label={"First Name"}
                    value={firstName}
                    onChangeText={setFirstName}
                    required
                  />
                </View>
                <View style={styles.gridCol}>
                  <TextField
                    label={"Middle Name"}
                    value={middleName}
                    onChangeText={setMiddleName}
                  />
                </View>
                <View style={styles.gridCol}>
                  <TextField
                    label={"Last Name"}
                    value={lastName}
                    onChangeText={setLastName}
                    required
                  />
                </View>
              </View>

              <TextField
                label={"NRC / Passport / PACRA"}
                value={nrcNumber}
                onChangeText={setNrcNumber}
                required
              />

              <View style={styles.gridRow}>
                <View style={styles.gridCol}>
                  <SelectField
                    label={"Sex"}
                    value={sex}
                    onChange={setSex}
                    options={SEXES}
                    required
                  />
                </View>
                <View style={styles.gridCol}>
                  <ThemedText type="defaultSemiBold" style={{ marginBottom: 8 }}>Date of Birth</ThemedText>
                  <Pressable
                    onPress={() => setShowDatePicker(true)}
                    style={{
                      borderWidth: 1,
                      borderColor: Colors[theme].border,
                      borderRadius: 38,
                      paddingHorizontal: 18,
                      paddingVertical: 14,
                      backgroundColor: Colors[theme].surface
                    }}
                  >
                    <ThemedText style={{ color: dateOfBirth ? Colors[theme].text : Colors[theme].muted }}>
                      {dateOfBirth || "YYYY-MM-DD"}
                    </ThemedText>
                  </Pressable>
                  {showDatePicker && (
                    <DateTimePicker
                      value={dateOfBirth ? new Date(dateOfBirth) : new Date(2000, 0, 1)}
                      mode="date"
                      display={Platform.OS === 'ios' ? 'spinner' : 'default'}
                      onChange={(event, selectedDate) => {
                        setShowDatePicker(false);
                        if (selectedDate) {
                          const yyyy = selectedDate.getFullYear();
                          const mm = String(selectedDate.getMonth() + 1).padStart(2, '0');
                          const dd = String(selectedDate.getDate()).padStart(2, '0');
                          setDateOfBirth(`${yyyy}-${mm}-${dd}`);
                        }
                      }}
                    />
                  )}
                </View>
                <View style={styles.gridCol}>
                  <SelectField
                    label={"Grower Category"}
                    value={category}
                    onChange={setCategory}
                    options={CATEGORIES}
                    required
                  />
                </View>
              </View>

              <ThemedText type="defaultSemiBold" style={{ marginBottom: -6 }}>Phone Number *</ThemedText>
              <View style={{ flexDirection: 'row', gap: 12, alignItems: 'flex-start' }}>
                <View style={{ width: 155 }}>
                  <SelectField
                    label={"Country"}
                    value={phoneCountryCode}
                    onChange={setPhoneCountryCode}
                    options={PHONE_CODES}
                    required
                  />
                </View>
                <View style={{ flex: 1 }}>
                  <TextField
                    label={"Local Number"}
                    value={phoneNumberLocal}
                    onChangeText={(text) => {
                      let val = text.replace(/[^0-9]/g, '');
                      setPhoneNumberLocal(val);
                    }}
                    required
                  />
                </View>
              </View>

              <TextField
                label={"Email Address"}
                value={email}
                onChangeText={setEmail}
                placeholder="name@example.com"
              />

              <TextField
                label={"Address / Farm / Plot Number"}
                value={address}
                onChangeText={setAddress}
                required
              />

              <TextField
                label={"Town / Village"}
                value={townOrVillage}
                onChangeText={setTownOrVillage}
                required
              />

              <View style={styles.gridRow}>
                <View style={styles.gridCol}>
                  <SelectField
                    label={"Province"}
                    value={province}
                    onChange={setProvince}
                    options={PROVINCES}
                    required
                  />
                </View>
                <View style={styles.gridCol}>
                  <SelectField
                    label={"District"}
                    value={district}
                    onChange={setDistrict}
                    options={districts}
                    disabled={!province}
                    required
                  />
                </View>
              </View>

              <View style={styles.gridRow}>
                <View style={styles.gridCol}>
                  <TextField
                    label={
                      <>
                        <MaterialIcons name="place" size={14} color="#111827" /> GPS Latitude <Chip label="Auto" />
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

              <ThemedText type="defaultSemiBold" style={styles.sectionLabel}>
                Identity Documents
              </ThemedText>

              <View style={styles.gridRow}>
                <View style={styles.gridCol}>
                  <ThemedText type="defaultSemiBold">Profile Photo *</ThemedText>
                  <FileDrop
                    label="Take / Upload Photo"
                    icon="photo-camera"
                    pickedUri={profilePhotoUri}
                    onPick={() => handleMediaSelection("front", setProfilePhotoUri)}
                  />
                </View>
                <View style={styles.gridCol}>
                  <ThemedText type="defaultSemiBold">ID Front *</ThemedText>
                  <FileDrop
                    label="ID Front"
                    icon="credit-card"
                    pickedUri={idFrontUri}
                    onPick={() => handleMediaSelection("back", setIdFrontUri)}
                  />
                </View>
                <View style={styles.gridCol}>
                  <ThemedText type="defaultSemiBold">ID Back *</ThemedText>
                  <FileDrop
                    label="ID Back"
                    icon="credit-card"
                    pickedUri={idBackUri}
                    onPick={() => handleMediaSelection("back", setIdBackUri)}
                  />
                </View>
              </View>

              <View style={styles.divider} />
              <InfoBanner left="Submitted by: TBZ Inspector" right={`Date: ${auditDateTime}`} />
            </View>
          </View>

          <View style={styles.actions}>
            <Pressable
              style={[styles.btn, styles.btnSecondary]}
              onPress={() => router.replace("/registration")}
            >
              <MaterialIcons name="close" size={18} color="#111827" />
              <ThemedText type="defaultSemiBold">Cancel</ThemedText>
            </Pressable>

            <Pressable
              style={[
                styles.btn,
                styles.btnPrimary,
              ]}
              onPress={async () => {
                const missing: string[] = [];
                if (!growerType) missing.push("Grower Type");
                if (!firstName.trim()) missing.push("First Name");
                if (!lastName.trim()) missing.push("Last Name");
                if (!nrcNumber.trim()) missing.push("NRC / Passport / PACRA");
                if (!sex) missing.push("Sex");
                if (!category) missing.push("Grower Category");
                if (!phoneNumberLocal.trim()) missing.push("Phone");
                if (!address.trim()) missing.push("Address / Farm / Plot Number");
                if (!townOrVillage.trim()) missing.push("Town / Village");
                if (!province) missing.push("Province");
                if (!district) missing.push("District");
                if (!profilePhotoUri) missing.push("Profile Photo");
                if (!idFrontUri) missing.push("ID Front");
                if (!idBackUri) missing.push("ID Back");

                if (missing.length > 0) {
                  Alert.alert(
                    "Missing fields",
                    `Please complete: ${missing.join(", ")}.`,
                  );
                  return;
                }
                if (!growerType) return;

                const parsedCode = phoneCountryCode.match(/\((\d+)\)/)?.[1] || "260";
                let finalPhone = parsedCode + phoneNumberLocal.trim().replace(/^0+/, "");

                const draft: GrowerRegistrationDraft = {
                  step: 1,
                  growerType,
                  firstName,
                  middleName,
                  lastName,
                  nrcNumber,
                  sex,
                  dateOfBirth,
                  category,
                  phoneNumber: finalPhone,
                  email,
                  address,
                  townOrVillage,
                  province,
                  district,
                  gpsLatitude,
                  gpsLongitude,
                  profilePhotoUri,
                  idFrontUri,
                  idBackUri,
                  auditDateTime,
                  editingId,
                };
                try {
                  const payload = JSON.stringify(draft);
                  let saved = false;
                  for (let attempt = 0; attempt < 5; attempt += 1) {
                    await AsyncStorage.setItem(DRAFT_KEY, payload);
                    const check = await AsyncStorage.getItem(DRAFT_KEY);
                    if (check) {
                      saved = true;
                      break;
                    }
                    await new Promise((r) => setTimeout(r, 120));
                  }

                  if (!saved) {
                    Alert.alert(
                      "Storage error",
                      "Could not save Step 1 data. Please try again.",
                    );
                    return;
                  }

                  router.push("/registration/crop");
                } catch {
                  Alert.alert(
                    "Storage error",
                    "Could not save Step 1 data. Please try again.",
                  );
                }
              }}
            >
              <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">
                Next
              </ThemedText>
              <MaterialIcons name="chevron-right" size={20} color="#FFFFFF" />
            </Pressable>
          </View>
        </View>
      </ScrollView>
      )}
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
  gridRow: { flexDirection: "row", gap: 12, flexWrap: "wrap" },
  gridCol: { flexGrow: 1, flexBasis: 220, gap: 8 },
  divider: { height: 1, backgroundColor: "#E5E7EB" },
  sectionLabel: { fontSize: 14 },
  infoBanner: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#E8F3EE",
    borderRadius: 12,
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
  radioGroup: { flexDirection: "row", gap: 12, flexWrap: "wrap" },
  radioItem: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  fileDrop: {
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    borderWidth: 1,
    borderStyle: "dashed",
    borderColor: "#D1D5DB",
    borderRadius: 14,
    paddingVertical: 16,
    paddingHorizontal: 10,
    backgroundColor: "#F9FAFB",
  },
  fileDropLabel: { textAlign: "center" },
  fileDropHint: { fontSize: 12, textAlign: "center" },
  retrieveButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    paddingVertical: 12,
    borderWidth: 1,
    borderRadius: 12,
  },
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
  btnPrimary: { backgroundColor: Colors.light.primary, borderColor: Colors.light.primary },
  modalBackdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.5)" },
  modalSheet: {
    maxHeight: "80%",
    backgroundColor: "#FFFFFF",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    overflow: "hidden",
  },
  modalHeader: {
    paddingHorizontal: 16,
    paddingVertical: 14,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    borderBottomWidth: 1,
    borderBottomColor: "#E5E7EB",
  },
  modalBody: { padding: 16, gap: 12 },
  modalAction: {
    backgroundColor: TOBACCO_ZM_GREEN,
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: "center",
    justifyContent: "center",
  },
});
