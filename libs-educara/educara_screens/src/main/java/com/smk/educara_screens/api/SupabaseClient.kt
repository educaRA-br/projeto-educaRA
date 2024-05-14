package com.smk.educara_screens.api

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://zdeytzpkytrevbzbuvmx.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkZXl0enBreXRyZXZiemJ1dm14Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTIxMDYxOTEsImV4cCI6MjAyNzY4MjE5MX0.BuGSESXaC5ENNKXKOqAS4tbmUfym1fFlZz63VWS2rVI"
) {
    install(Postgrest)
}