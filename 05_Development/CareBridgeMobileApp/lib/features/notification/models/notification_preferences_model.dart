import 'notification_model.dart';

class NotificationPreferences {
  const NotificationPreferences({
    required this.preferences,
    required this.appointmentReminderDefaults,
  });

  final List<NotificationPreference> preferences;
  final List<int> appointmentReminderDefaults;
}
