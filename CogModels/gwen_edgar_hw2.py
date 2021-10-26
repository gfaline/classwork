import math
import csv


def bystander_model(n, temperature, altruism=.5):
    if altruism == 1:
        return 1
    if altruism >= .9 and temperature > 10:
        return 1
    if altruism < .1:
        return 0

    if n > 4:
        if altruism > 0.6:
            if temperature > 45:
                return 1
        return 0
    if altruism > .3 and temperature > 32:
        return 1
    return 0


def error_metric(filename):
    # this function accepts the file name of a .csv file
    # here comes your code, returning the value of your metric
    # it will probably need to call your bystander_model function at some point
    # 1/n * sum(value-prediction)^2
    sum = 0
    entries = 0
    f = open(filename, 'r')
    reader = csv.DictReader(f)
    for row in reader:
        entries += 1
        prediction = bystander_model(int(row['numberofbystanders']), int(row['ambienttemperature']))
        outcome = int(row['saved'])
        sum += (outcome-prediction)**2
        # print("N: ", int(row['numberofbystanders']), ", A: ", int(row['ambienttemperature']), ", p: ", prediction,
        #       ", R ", outcome)
    if entries == 0:
        return 0
    if sum == 0:
        return 0
    sum = sum/entries
    return sum


false_error = error_metric("gwen_edgar_falsifying_data.csv")
print("The falsifying error number is ", false_error)
true_error = error_metric("gwen_edgar_supporting_data.csv")

print("The non falasiying error number is ", true_error)
