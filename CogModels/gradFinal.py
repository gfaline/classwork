import random
import matplotlib.pyplot as plt
from matplotlib.colors import LinearSegmentedColormap


def predict_trustworthyness(company_trustworthiness, num_cameras, physical_appearance):
    physical_appearance_effect = physical_appearance
    if physical_appearance < 85 and physical_appearance > 75:

        physical_appearance_effect = physical_appearance - ((physical_appearance - 75) * 12)

    if physical_appearance >= 85 and physical_appearance < 98:
        physical_appearance_effect = physical_appearance - 115 + (physical_appearance - 85) ** 1.9

    return max(0,
               (company_trustworthiness - (num_cameras ** 0.5) + 1.4) / 10.0 * (physical_appearance_effect + 25) / 100)


def generate_random_data(n):
    data = []
    while len(data) < n:
        company_trustworthiness = random.randint(1, 10)
        num_cameras = random.randint(1, 25)
        physical_appearance = random.randint(1, 100)

        if (company_trustworthiness, num_cameras, physical_appearance) not in data:
            data.append((company_trustworthiness, num_cameras, physical_appearance))

    return data


n = 600
X = []
Y1 = []
Y2 = []
Y3 = []

data = generate_random_data(n)
for (company_trustworthiness, num_cameras, physical_appearance) in data:
    num = predict_trustworthyness(company_trustworthiness, num_cameras, physical_appearance)
    print((company_trustworthiness, num_cameras, physical_appearance), num)
    X.append(num)
    Y1.append(company_trustworthiness)
    Y2.append(num_cameras)
    Y3.append(physical_appearance)

print(X)
# plt.plot(Y2,X,"o")

cmap = LinearSegmentedColormap.from_list('rg', ["r", "w", "g"], N=256)
cmap = LinearSegmentedColormap.from_list("", ["red", "green"])

fig = plt.figure()
ax = plt.axes(projection='3d')
ax.scatter3D(Y1, Y2, Y3, c=X, cmap="inferno")
ax.set_xlabel("company_trustworthiness")
ax.set_ylabel("num_cameras")
ax.set_zlabel("physical_appearance")

plt.show()