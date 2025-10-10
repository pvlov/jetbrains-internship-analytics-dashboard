import csv
import random
from typing import Any
from pathlib import Path
from faker import Faker

NUM_RECORDS = 100_000
OUTPUT_FILE: Path = Path() / "out" / "fake-titanic.csv"

fake = Faker()


def generate_dummy_passengers(num_records: int) -> list[dict[str, Any]]:
    """Generates a list of dummy passenger records."""
    passengers = []
    print(f"Generating {num_records} records... this may take a moment.")

    for i in range(num_records):
        sex = random.choice(["male", "female"])
        name = fake.name_male() if sex == "male" else fake.name_female()

        passenger = {
            "survived": random.choice([True, False]),
            "pclass": random.randint(1, 3),
            "name": name,
            "sex": sex,
            "age": round(random.uniform(0.5, 80.0), 1) if random.random() > 0.15 else None,
            "sibsp": random.randint(0, 8),
            "parch": random.randint(0, 6),
            "ticket": f"{random.choice(['PC', 'A/5', 'C.A.', 'STON/O2.'])} {random.randint(1000, 45000)}",
            "fare": round(random.uniform(0.0, 512.0), 2),
            "cabin": f"{random.choice(['A', 'B', 'C', 'D', 'E', 'F', 'G'])}{random.randint(1, 100)}"
            if random.random() > 0.75
            else None,
            "embarked": random.choice(["S", "C", "Q"]),
        }
        passengers.append(passenger)

        if (i + 1) % 10_000 == 0:
            print(f"  ...generated {i + 1}/{num_records} records")

    return passengers


def write_to_csv(data: list[dict[str, Any]], filename: Path) -> None:
    """Writes the generated data to a CSV file."""
    if not data:
        print("No data to write.")
        return

    header = data[0].keys()

    filename.parent.mkdir(parents=True, exist_ok=True)
    filename.touch(exist_ok=True)

    with filename.open("w", newline="", encoding="utf-8") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=header)
        writer.writeheader()
        writer.writerows(data)

    print("Done.")


if __name__ == "__main__":
    dummy_data = generate_dummy_passengers(NUM_RECORDS)
    write_to_csv(dummy_data, OUTPUT_FILE)
