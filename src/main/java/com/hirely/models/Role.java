package com.hirely.models;

/**
 * Role enum representing user roles in the system
 */
public enum






Role {
    ADMIN("ADMIN", 1),
    RECRUITER("RECRUITER", 2),
    INTERVIEWEE("INTERVIEWEE", 3);

    private final String roleName;
    private final int roleId;

    Role(String roleName, int roleId) {
        this.roleName = roleName;
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public int getRoleId() {
        return roleId;
    }

    /**
     * Get Role enum from role ID
     */
    public static Role fromId(int roleId) {
        for (Role role : Role.values()) {
            if (role.getRoleId() == roleId) {
                return role;
            }
        }
        return null;
    }

    /**
     * Get Role enum from role name
     */
    public static Role fromName(String roleName) {
        for (Role role : Role.values()) {
            if (role.getRoleName().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return null;
    }
}
