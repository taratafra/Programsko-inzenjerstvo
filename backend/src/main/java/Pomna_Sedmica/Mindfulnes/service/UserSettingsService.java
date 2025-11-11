@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSettingsResponseDTO getUserSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserSettingsResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.isSocialLogin(),
                user.isFirstLogin()
        );
    }

    @Transactional
    public UserSettingsResponseDTO updateUserSettings(String email, UpdateUserSettingsRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if they are provided
        if (request.name() != null) user.setName(request.name());
        if (request.surname() != null) user.setSurname(request.surname());
        if (request.bio() != null) user.setBio(request.bio());
        if (request.profilePictureUrl() != null) user.setProfilePictureUrl(request.profilePictureUrl());

        user.setLastModifiedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return new UserSettingsResponseDTO(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getSurname(),
                savedUser.getBio(),
                savedUser.getProfilePictureUrl(),
                savedUser.isSocialLogin(),
                savedUser.isFirstLogin()
        );
    }

    /**
     * Reset password for first-time login users (local authentication only)
     */
    @Transactional
    public boolean resetFirstTimePassword(String email, FirstTimePasswordResetRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isFirstLogin() || user.isSocialLogin()) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false);
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }

    /**
     * Change password for existing local users (not first-time)
     */
    @Transactional
    public boolean changePassword(String email, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isSocialLogin()) {
            return false;
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setLastModifiedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }

    /**
     * Check if user requires first-time password reset (local users only)
     */
    public boolean isFirstLoginRequired(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.isFirstLogin() && !user.isSocialLogin();
    }
}