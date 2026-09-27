const configuredClinicalOnly = import.meta.env.VITE_CLINICAL_ONLY_PILOT;

export const clinicalOnlyPilot = import.meta.env.PROD
  ? configuredClinicalOnly !== "false"
  : configuredClinicalOnly === "true";

const configuredClinicalData = import.meta.env.VITE_CLINICAL_DATA_ENABLED;
const configuredClinicalDataReleaseApproval = import.meta.env.VITE_CLINICAL_DATA_RELEASE_APPROVED;

export function clinicalDataAccessGranted(
  isProduction: boolean,
  dataEnabled: string | undefined,
  releaseApproved: string | undefined,
): boolean {
  if (!isProduction) return dataEnabled !== "false";
  return dataEnabled === "true" && releaseApproved === "true";
}

export const clinicalDataEnabled = clinicalDataAccessGranted(
  import.meta.env.PROD,
  configuredClinicalData,
  configuredClinicalDataReleaseApproval,
);

export const clinicalDataRoutes = new Set([
  "/dashboard",
  "/agenda",
  "/patients",
  "/clinical-records",
  "/finance",
  "/compliance",
]);

export const pilotUnavailableRoutes = new Set([
  "/packages",
  "/subscriptions",
  "/fiscal",
  "/notifications",
  "/billing",
]);
