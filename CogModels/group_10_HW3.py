import math
import csv


def probabilistic_bystander_model(n, temperature, altruism=.5):
    if altruism == 1:
        return 1
    if altruism >= .9 and temperature > 10:
        return 1
    if altruism < .1:
        return 0.001

    if n > 4:
        if altruism > 0.6:
            if 200 > temperature > 45:
                return 1
        return 0.001
    if altruism > .3 and 200 > temperature > 32:
        return 1
    return 0.001


def error_metric(filename):
    sum = 0
    entries = 0
    f = open(filename, 'r')
    reader = csv.DictReader(f)
    for row in reader:
        entries += 1
        prediction = probabilistic_bystander_model(int(row['numberofbystanders']), int(row['ambienttemperature']))
        outcome = int(row['saved'])