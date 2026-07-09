import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useNavigation } from "@react-navigation/native";
import { Stack, useRouter } from "expo-router";
import React from "react";
import { Pressable } from "react-native";
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

export default function InspectionLayout() {
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const router = useRouter();
  const navigation = useNavigation();
  return (
    <Stack
      screenOptions={({ route }) => ({
        headerShown: true,
        headerLargeTitle: false,
        headerStyle: { backgroundColor: Colors[theme].primary },
        headerTintColor: Colors[theme].surface,
        headerTitleAlign: "left",
        headerTitleStyle: { fontSize: 22, fontWeight: "600", color: Colors[theme].surface },
        headerShadowVisible: false,
        headerBackTitleVisible: false,
        headerLeft: () => (
          <Pressable
            onPress={() => (navigation.canGoBack() ? router.back() : router.replace("/"))}
            style={{ paddingHorizontal: 16, paddingVertical: 8 }}
            hitSlop={10}
          >
            <MaterialIcons name="arrow-back" size={24} color={Colors[theme].surface} />
          </Pressable>
        ),
      })}
    >
      <Stack.Screen name="index" options={{ title: "Inspection" }} />
      <Stack.Screen name="detail" options={{ title: "Inspection Details" }} />
      <Stack.Screen name="field" options={{ title: "Field Inspection" }} />
      <Stack.Screen name="nursery" options={{ title: "Nursery Inspection" }} />
      <Stack.Screen name="curing" options={{ title: "Curing Inspection" }} />
      <Stack.Screen name="schedule" options={{ title: "Schedule" }} />
      <Stack.Screen name="reports" options={{ title: "Reports" }} />
      <Stack.Screen name="high-risk" options={{ title: "High Risk" }} />
      <Stack.Screen name="lookup" options={{ title: "Lookup" }} />
      <Stack.Screen name="portal-list" options={{ title: "Portal Inspections" }} />
      <Stack.Screen name="schedules-local" options={{ title: "Local Schedules" }} />
    </Stack>
  );
}
