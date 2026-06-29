import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useNavigation } from "@react-navigation/native";
import { Stack, useRouter } from "expo-router";
import React from "react";
import { Pressable } from "react-native";
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

export default function RegistrationLayout() {
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
      <Stack.Screen name="index" options={{ title: "Growers" }} />
      <Stack.Screen name="growers-list" options={{ title: "Growers" }} />
      <Stack.Screen name="grower-details" options={{ title: "Grower Details" }} />
      <Stack.Screen name="grower-edit" options={{ title: "Edit Grower" }} />
      <Stack.Screen name="new" options={{ title: "New Grower" }} />
      <Stack.Screen name="crop" options={{ title: "Crops" }} />
      <Stack.Screen name="crop-allocation" options={{ title: "Crop Allocation" }} />
    </Stack>
  );
}
