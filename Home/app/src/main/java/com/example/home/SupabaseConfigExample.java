package com.example.home;

public class SupabaseConfigExample {

    // =============================================
    // ВНИМАНИЕ: Замените эти значения на свои реальные ключи!
    // Скопируйте этот файл в SupabaseConfig.java и заполните данные
    // =============================================

    // Базовый URL Supabase (без указания конкретной таблицы)
    public static final String SUPABASE_BASE_URL = "https://your-project-ref.supabase.co/rest/v1";
    // API ключ для доступа к Supabase (anon/public ключ)
    public static final String SUPABASE_API_KEY = "your-actual-anon-key-here";

    // Имена таблиц (можете оставить без изменений)
    public static final String TABLE_USERS = "/users";
    public static final String TABLE_PETS = "/pets";
    public static final String TABLE_CHATS = "/chats";
    public static final String TABLE_MESSAGES = "/messages";
}