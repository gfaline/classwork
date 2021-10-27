import math
import csv
import matplotlib.pyplot as plt


def probabilistic_bystander_model(n, temperature, altruism=.5):
    if altruism == 1:
        return 1
    if altruism >= .9 and temperature > 10:
        return 1
    if altruism < .1:
        return 0.001

    if n > 4:
        if altruism > 0.6:
            if 110 > temperature > 45:
                return higher_altruism(temperature)
        return 0.001
    if altruism > .3 and 110 > temperature > 32:
        return lower_altruism(temperature)
    return 0.001


def lower_altruism(temperature):
    if temperature < 45:
        return 0.001
    if 55 > temperature >= 45:
        return 0.001
    if 60 > temperature >= 55:
        return 0.1
    if 65 > temperature >= 60:
        return 0.15
    if 70 > temperature >= 65:
        return 0.2
    if 75 > temperature >= 70:
        return 0.45
    if 80 > temperature >= 75:
        return 0.5
    if 85 > temperature >= 80:
        return 0.45
    if 90 > temperature >= 85:
        return 0.2
    if 95 > temperature >= 90:
        return 0.05
    if 100 > temperature >= 95:
        return 0.001
    if temperature >= 100:
        return 0.001


def higher_altruism(temperature):
    if temperature < 45:
        return 0.001
    if 55 > temperature >= 45:
        return 0.05
    if 60 > temperature >= 55:
        return 0.1
    if 65 > temperature >= 60:
        return 0.2
    if 70 > temperature >= 65:
        return 0.5
    if 75 > temperature >= 70:
        return 0.6
    if 80 > temperature >= 75:
        return 0.8
    if 85 > temperature >= 80:
        return 0.7
    if 90 > temperature >= 85:
        return 0.3
    if 95 > temperature >= 90:
        return 0.1
    if 100 > temperature >= 95:
        return 0.05
    if temperature >= 100:
        return 0.001


def minus_log_likelihood(filename, altruism=.5):
    sum = 0
    entries = 0
    f = open(filename, 'r')
    reader = csv.DictReader(f)
    for row in reader:
        entries += 1
        prediction = probabilistic_bystander_model(int(row['numberofbystanders']), int(row['ambienttemperature']), altruism)
        outcome = int(row['saved'])
        sum += math.log(math.abs(prediction - outcome), 10)

    sum *= -1
    return sum


def LL_as_func_A():
    A = []
    LL = []
    for i in range(1, 100):
        A.append(i / 100)
        LL.append(minus_log_likelihood("empirical.csv", i/100))
    plt.plot(A, LL)
    plt.show()

