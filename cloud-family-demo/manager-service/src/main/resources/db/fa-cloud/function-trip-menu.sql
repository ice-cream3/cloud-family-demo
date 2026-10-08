USE `fa-cloud`;

INSERT IGNORE INTO sys_permission (permission_code, permission_name, description, status)
VALUES
    ('partner:trip:add', 'Trip Booking Add', 'Allows creating trip booking records', 'ENABLED'),
    ('partner:trip:edit', 'Trip Booking Edit', 'Allows updating trip booking records', 'ENABLED'),
    ('partner:trip:delete', 'Trip Booking Delete', 'Allows deleting trip booking records', 'ENABLED');

INSERT IGNORE INTO sys_menu (menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
VALUES ('function-management', 'Function Management', '/api/manager/function', 'FunctionManagement', 'Grid2X2', 1, 0, 50, 1, 'ENABLED');

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id,
       'trip-booking-management',
       'Trip Booking Management',
       '/api/manager/function/trips',
       'TripBookingManagement',
       'CalendarDays',
       2,
       0,
       51,
       1,
       'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'function-management';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, item.menu_code, item.menu_name, item.path, item.component, item.icon, 3, 0, item.sort_order, 1, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'trip-booking-users' menu_code, 'Booking Users' menu_name, '/api/manager/function/trips/users' path, 'TripBookingUsers' component, 'UserCheck' icon, 511 sort_order
    UNION ALL SELECT 'trip-booking-publishes', 'Booking Publishes', '/api/manager/function/trips/publishes', 'TripBookingPublishes', 'CalendarPlus', 512
    UNION ALL SELECT 'trip-booking-reviews', 'Booking Reviews', '/api/manager/function/trips/reviews', 'TripBookingReviews', 'ClipboardCheck', 513
) item ON 1 = 1
WHERE parent.menu_code = 'trip-booking-management';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, CONCAT(parent.menu_code, '-', item.action_code), item.menu_name, item.permission_code, 'ButtonPermission', 'MousePointerClick', 4, 1, parent.sort_order * 10 + item.sort_offset, 0, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'add' action_code, 'Add' menu_name, 'partner:trip:add' permission_code, 1 sort_offset
    UNION ALL SELECT 'edit', 'Edit', 'partner:trip:edit', 2
    UNION ALL SELECT 'delete', 'Delete', 'partner:trip:delete', 3
) item ON 1 = 1
WHERE parent.menu_code IN ('trip-booking-publishes', 'trip-booking-reviews');

INSERT IGNORE INTO sys_menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM sys_menu m
INNER JOIN sys_permission p ON p.permission_code = m.path
WHERE m.menu_code LIKE 'trip-booking-%'
  AND m.button_flag = 1;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('partner:trip:add', 'partner:trip:edit', 'partner:trip:delete');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND (
      m.menu_code IN ('function-management', 'trip-booking-management', 'trip-booking-users', 'trip-booking-publishes', 'trip-booking-reviews')
      OR m.menu_code LIKE 'trip-booking-publishes-%'
      OR m.menu_code LIKE 'trip-booking-reviews-%'
  );
