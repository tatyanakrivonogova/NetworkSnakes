package lab4.network.messages;

import lab4.network.node.Role;

public class RoleChangeMessage extends Message {
    private final Role oldRole;
    private final Role newRole;

    public RoleChangeMessage(Role oldRole, Role newRole) {
        super(MessageType.ROLE_CHANGE);
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public Role getOldRole() {
        return oldRole;
    }

    public Role getNewRole() {
        return newRole;
    }
}