package org.valipuz;

import org.valipuz.config.AppConfig;
import org.valipuz.controller.MainMenu;
import org.valipuz.controller.UserMenu;
import org.valipuz.repository.LinkRepository;
import org.valipuz.repository.UserRepository;
import org.valipuz.repository.impl.LinkRepositoryImpl;
import org.valipuz.repository.impl.UserRepositoryImpl;
import org.valipuz.service.LinkService;
import org.valipuz.service.UserService;
import org.valipuz.utils.CodeGenerator;

import java.time.Clock;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig("application.properties");
            int ttlHours = config.getInt("link.ttl.hours");
            String domain = config.getString("link.domain");

            Scanner scanner = new Scanner(System.in);

            UserRepository userRepository = new UserRepositoryImpl(config.getString("storage.users.file"));
            LinkRepository linkRepository = new LinkRepositoryImpl(config.getString("storage.links.file"));

            CodeGenerator codeGenerator = new CodeGenerator(config.getInt("link.code.length"));
            UserService userService = new UserService(userRepository);
            LinkService linkService = new LinkService(userService, linkRepository, codeGenerator, Clock.systemUTC(), ttlHours,
                    config.getInt("link.limit.default"), config.getInt("link.limit.max"), domain);

            UserMenu userMenu = new UserMenu(scanner, userService, linkService);
            MainMenu mainMenu = new MainMenu(scanner, userMenu, linkService);

            int cleanupMinutes = config.getInt("cleanup.interval.minutes");
            // шедулер для удаления ссылок по TTL
            try(ScheduledExecutorService deleteExpiredLinksScheduler =
                    Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "expired-link-cleaner"))
            ) {
                deleteExpiredLinksScheduler.scheduleAtFixedRate(() -> {
                    try {
                        linkService.removeExpired();
                    } catch (RuntimeException e) {
                        // без этого первое же исключение навсегда остановит очистку
                        System.out.println("Ошибка очистки ссылок: " + e.getMessage());
                    }
                }, 0, cleanupMinutes, TimeUnit.MINUTES);
                mainMenu.runMainMenu();
            }
        } catch (IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
