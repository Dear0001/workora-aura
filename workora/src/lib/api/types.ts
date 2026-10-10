// Shared ApiResponse<T> envelope — matches backend contract
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// Auth types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  emailVerified: boolean;
}

export interface RegisterResponse {
  userId: string;
  email: string;
  verificationSessionToken: string;
  message: string;
}

export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  emailVerified: boolean;
}

export interface VerificationSessionResponse {
  status: 'PENDING' | 'VERIFIED' | 'EXPIRED';
  expiresAt: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  gender?: string;
  dateOfBirth?: string;
  avatarUrl?: string;
  bio?: string;
  experience?: string;
  role: string;
  emailVerified: boolean;
  accountStatus: string;
}

// Organization types
export interface Company {
  id: string;
  name: string;
  description?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  profileImageUrl?: string;
  coverImageUrl?: string;
  ownerUserId?: string;
  averageRating?: number;
  ratingCount?: number;
  status?: string;
  createdAt?: string;
}

export interface Department {
  id: string;
  name: string;
  description?: string;
  companyId: string;
  createdAt?: string;
}

export interface CompanyMember {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  departmentId?: string;
  departmentName?: string;
  departmentMemberships?: { departmentId: string; role: string }[];
  joinedAt?: string;
  status?: string;
}

export interface Invitation {
  id: string;
  email: string;
  role: string;
  companyId: string;
  companyName?: string;
  departmentId?: string;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELLED';
  createdAt?: string;
  expiresAt?: string;
}

export type InviteableCompanyRole =
  'ADMIN' | 'PROJECT_MANAGER' | 'DEVELOPER' | 'TESTER' | 'BUG_HUNTER';

export interface InviteMemberRequest {
  email: string;
  role: InviteableCompanyRole;
  departmentId?: string;
  message?: string;
}

export interface CreateDepartmentRequest {
  name: string;
  description?: string;
}

// Pagination
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface DashboardSummary {
  recentActivity: DashboardActivityItem[];
  tasksDueToday: number;
  openBugReports: number;
  activeProjects: number;
  teamMembers: number;
  unreadNotifications: number;
  phaseCompletion: number;
  taskStatusBreakdown: {
    open: number;
    inProgress: number;
    inReview: number;
    done: number;
  };
  taskActivity: { date: string; completed: number; created: number }[];
  activeProjectsList: DashboardProject[];
}

export interface AdminDashboardSummary {
  accounts: number;
  companies: number;
  projects: number;
  tasks: number;
  bugReports: number;
  recruitmentPosts: number;
  notifications: number;
  payments: number;
}

export interface AdminServiceHealth {
  service: string;
  status: string;
  checkedAt: string;
  components: Record<string, string>;
}

export interface AdminAccessRequest {
  id: string;
  email: string;
  requestedBy: string;
  decidedBy?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  decidedAt?: string;
}

export interface AdminCompany {
  id: string;
  name: string;
  ownerUserId: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';
  createdAt: string;
}

export interface AdminSubscription {
  userId: string;
  plan: 'FREE' | 'PRO' | 'ENTERPRISE';
  maxCompanies: number;
  entitled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminMembershipActivity {
  id: string;
  companyId: string;
  userId: string;
  actorUserId: string;
  action: string;
  roleBefore?: string;
  roleAfter?: string;
  details?: string;
  createdAt: string;
}

export interface DashboardActivityItem {
  id: string;
  type: string;
  message: string;
  occurredAt?: string;
  meta?: string;
}

export interface DashboardProject {
  id: string;
  name: string;
  status: string;
  phaseCompletion: number;
  members: number;
  tasksOpen: number;
  tasksDone: number;
  dueDate?: string;
  priority?: string;
}
