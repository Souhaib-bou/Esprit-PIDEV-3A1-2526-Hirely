package Test;

import Models.JobOffer;
import Services.JobOfferService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class MainConsole {

    public static void main(String[] args) {

        JobOfferService service = new JobOfferService();

        try {

            // 1️⃣ CREATE
            JobOffer j = new JobOffer(
                    "Java Developer",
                    "Spring Boot Backend Developer",
                    "CDI",
                    3000.0,
                    "Tunis",
                    2,
                    Date.valueOf(LocalDate.now()),
                    "Open",1
            );

            service.add(j);
            System.out.println("✅ Job added successfully");

            // 2️⃣ READ
            List<JobOffer> list = service.getAll();

            System.out.println("📌 All Job Offers:");
            for (JobOffer job : list) {
                System.out.println(job.getJobOfferId() + " - " + job.getTitle());
            }

            // 3️⃣ UPDATE (update first record)
            if (!list.isEmpty()) {
                JobOffer first = list.get(0);
                first.setSalary(4000);
                service.update(first);
                System.out.println("✅ Updated successfully");
            }

            // 4️⃣ DELETE (delete last record)
            if (!list.isEmpty()) {
                JobOffer last = list.get(list.size() - 1);
                service.delete(last.getJobOfferId());
                System.out.println("✅ Deleted successfully");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
